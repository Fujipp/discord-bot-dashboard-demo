package project.discord.backend.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.runtime.dto.RuntimeCommandResponse;
import project.discord.backend.runtime.dto.RuntimeHostMetricsResponse;
import project.discord.backend.runtime.dto.RuntimeHostOptionResponse;
import project.discord.backend.runtime.dto.RuntimeProcessResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class Pm2RuntimeService {

    private static final Pattern SAFE_PROCESS_NAME = Pattern.compile("^[a-zA-Z0-9._:-]{1,80}$");
    private static final Set<String> ALLOWED_ACTIONS = Set.of("start", "stop", "restart");
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(12);
    private static final String PRIMARY_HOST_ID = "do-sgp1-primary";

    private final JsonMapper jsonMapper;
    private final String sshUser;
    private final String sshHost;
    private final String sshPort;
    private final String sshKeyPath;
    private final String pm2Binary;
    private final String hostName;
    private final String hostRegion;

    public Pm2RuntimeService(
            @Value("${app.bot-runner.ssh.user:root}") String sshUser,
            @Value("${app.bot-runner.ssh.host:localhost}") String sshHost,
            @Value("${app.bot-runner.ssh.port:22}") String sshPort,
            @Value("${app.bot-runner.ssh.key-path:}") String sshKeyPath,
            @Value("${app.bot-runner.pm2.binary:pm2}") String pm2Binary,
            @Value("${app.bot-runner.name:DigitalOcean Primary}") String hostName,
            @Value("${app.bot-runner.region:sgp1}") String hostRegion
    ) {
        this.jsonMapper = JsonMapper.builder().build();
        this.sshUser = sshUser;
        this.sshHost = sshHost;
        this.sshPort = sshPort;
        this.sshKeyPath = sshKeyPath;
        this.pm2Binary = pm2Binary;
        this.hostName = hostName;
        this.hostRegion = hostRegion;
    }

    public List<RuntimeProcessResponse> listProcesses() {
        return listProcesses(PRIMARY_HOST_ID);
    }

    public List<RuntimeHostOptionResponse> listHosts() {
        return List.of(new RuntimeHostOptionResponse(PRIMARY_HOST_ID, hostName, sshHost, hostRegion, true));
    }

    public String primaryHostId() {
        return PRIMARY_HOST_ID;
    }

    public List<RuntimeProcessResponse> listProcesses(String hostId) {
        CommandResult result = runRemote(hostId, pm2Binary + " jlist");
        if (result.exitCode() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not list pm2 processes");
        }

        try {
            JsonNode root = jsonMapper.readTree(result.output());
            List<RuntimeProcessResponse> processes = new ArrayList<>();

            for (JsonNode process : root) {
                JsonNode pm2Env = process.path("pm2_env");
                JsonNode monit = process.path("monit");
                Long uptimeMillis = uptimeMillis(pm2Env.path("pm_uptime").asLong(0L));

                processes.add(new RuntimeProcessResponse(
                        process.path("name").asText("unknown"),
                        pm2Env.path("status").asText("unknown"),
                        process.path("pid").asInt(0),
                        monit.path("cpu").asDouble(0),
                        monit.path("memory").asLong(0),
                        pm2Env.path("restart_time").asInt(0),
                        uptimeMillis
                ));
            }

            return processes;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not parse pm2 process list");
        }
    }

    public RuntimeHostMetricsResponse getHostMetrics() {
        return getHostMetrics(PRIMARY_HOST_ID);
    }

    public RuntimeHostMetricsResponse getHostMetrics(String hostId) {
        CommandResult result = runRemote(hostId, """
                disk_line=$(df -k / | awk 'NR==2{print $2","$3","$4","$5}');
                mem_line=$(free -k | awk '/^Mem:/{print $2","$3","$7}');
                swap_line=$(free -k | awk '/^Swap:/{print $2","$3}');
                load_line=$(awk '{print $1}' /proc/loadavg);
                uptime_line=$(uptime -p);
                printf 'disk=%s\\nmem=%s\\nswap=%s\\nload=%s\\nuptime=%s\\n' "$disk_line" "$mem_line" "$swap_line" "$load_line" "$uptime_line"
                """);

        if (result.exitCode() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not load runtime host metrics");
        }

        try {
            String disk = valueFor(result.output(), "disk");
            String mem = valueFor(result.output(), "mem");
            String swap = valueFor(result.output(), "swap");
            String load = valueFor(result.output(), "load");
            String uptime = valueFor(result.output(), "uptime");

            String[] diskParts = disk.split(",");
            String[] memParts = mem.split(",");
            String[] swapParts = swap.split(",");

            return new RuntimeHostMetricsResponse(
                    kilobytesToBytes(parseLong(diskParts, 0)),
                    kilobytesToBytes(parseLong(diskParts, 1)),
                    kilobytesToBytes(parseLong(diskParts, 2)),
                    parsePercent(diskParts.length > 3 ? diskParts[3] : "0%"),
                    kilobytesToBytes(parseLong(memParts, 0)),
                    kilobytesToBytes(parseLong(memParts, 1)),
                    kilobytesToBytes(parseLong(memParts, 2)),
                    kilobytesToBytes(parseLong(swapParts, 0)),
                    kilobytesToBytes(parseLong(swapParts, 1)),
                    Double.parseDouble(load),
                    uptime
            );
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not parse runtime host metrics");
        }
    }

    public RuntimeCommandResponse runAction(String processName, String action) {
        return runAction(PRIMARY_HOST_ID, processName, action);
    }

    public RuntimeCommandResponse runAction(String hostId, String processName, String action) {
        String safeAction = action.toLowerCase(Locale.ROOT);
        validateProcessName(processName);

        if (!ALLOWED_ACTIONS.contains(safeAction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported runtime action");
        }

        CommandResult result = runRemote(hostId, pm2Binary + " " + safeAction + " " + processName);

        if (result.exitCode() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "pm2 command failed: " + trimOutput(result.output()));
        }

        return new RuntimeCommandResponse(
                processName,
                safeAction,
                true,
                trimOutput(result.output()),
                listProcesses(hostId)
        );
    }

    private CommandResult runRemote(String hostId, String remoteCommand) {
        validateHostId(hostId);

        List<String> command = new ArrayList<>();
        command.add("ssh");
        command.add("-o");
        command.add("BatchMode=yes");
        command.add("-o");
        command.add("ConnectTimeout=8");
        command.add("-p");
        command.add(sshPort);

        if (!sshKeyPath.isBlank()) {
            command.add("-i");
            command.add(sshKeyPath);
        }

        command.add(sshUser + "@" + sshHost);
        command.add(remoteCommand);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            boolean completed = process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "pm2 runtime command timed out");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new CommandResult(process.exitValue(), output);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not connect to bot runtime host");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Runtime command was interrupted");
        }
    }

    private void validateProcessName(String processName) {
        if (processName == null || !SAFE_PROCESS_NAME.matcher(processName).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pm2 process name");
        }
    }

    private void validateHostId(String hostId) {
        if (!PRIMARY_HOST_ID.equals(hostId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Runtime host was not found");
        }
    }

    private Long uptimeMillis(Long startedAtMillis) {
        if (startedAtMillis == null || startedAtMillis <= 0) {
            return 0L;
        }

        return Math.max(0L, Instant.now().toEpochMilli() - startedAtMillis);
    }

    private String valueFor(String output, String key) {
        String prefix = key + "=";
        for (String line : output.split("\\R")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }

        return "";
    }

    private Long parseLong(String[] values, int index) {
        if (index >= values.length || values[index].isBlank()) {
            return 0L;
        }

        return Long.parseLong(values[index].trim());
    }

    private Long kilobytesToBytes(Long value) {
        return value * 1024L;
    }

    private Integer parsePercent(String value) {
        return Integer.parseInt(value.replace("%", "").trim());
    }

    private String trimOutput(String output) {
        if (output == null) {
            return "";
        }

        String compactOutput = output.trim();
        if (compactOutput.length() <= 1200) {
            return compactOutput;
        }

        return compactOutput.substring(0, 1200);
    }

    private record CommandResult(Integer exitCode, String output) {
    }
}
