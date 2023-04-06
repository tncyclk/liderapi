package tr.org.lider.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import tr.org.lider.entities.CommandExecutionImpl;
import tr.org.lider.entities.CommandExecutionResultImpl;
import tr.org.lider.entities.CommandImpl;
import tr.org.lider.entities.PluginTask;
import tr.org.lider.services.CommandService;
import tr.org.lider.services.ExcelExportService;
import tr.org.lider.services.PluginTaskService;
import tr.org.lider.services.ScheduledTaskReportService;

@Secured({ "ROLE_ADMIN", "ROLE_EXECUTED_TASK" })
@RestController
@RequestMapping("/api/lider/scheduled-task-report")
@Tag(name = "Scheduled Task Report", description = "Scheduled Task Report Service")
public class ScheduledTaskReportController {

	private final Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ScheduledTaskReportService scheduledTaskService;

	@Autowired
	private CommandService commandService;

	@Autowired
	private PluginTaskService pluginTaskService;

	@Autowired
	private ExcelExportService excelService;

	@Operation(summary = "Find all scheduled task list.", description = "", tags = { "scheduled-task-report" })
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Find all scheduled task list."),
			@ApiResponse(responseCode = "417", description = "Could not retrieve scheduled task list. Unexpected error occured.", 
			content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> findAllCommandsRest(@RequestParam(value = "pageNumber") int pageNumber,
			@RequestParam(value = "pageSize") int pageSize,
			@RequestParam(value = "taskCommand") Optional<String> taskCommand,
			@RequestParam(value = "startDate") @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss") Optional<Date> startDate,
			@RequestParam(value = "endDate") @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss") Optional<Date> endDate) {
		Page<CommandImpl> commands = scheduledTaskService.findAllCommandsFiltered(pageNumber, pageSize, taskCommand,
				startDate, endDate);
		for (CommandImpl command : commands.getContent()) {
			for (CommandExecutionImpl commandExecution : command.getCommandExecutions()) {
				for (CommandExecutionResultImpl commandExecutionResult : commandExecution
						.getCommandExecutionResults()) {
					if (commandExecutionResult.getResponseData() != null) {
						commandExecutionResult
								.setResponseDataStr((new String(commandExecutionResult.getResponseData())));
					}
				}
			}
		}
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(commands);
	}

	@Operation(summary = "Find all plugin tasks.", description = "", tags = { "scheduled-task-report" })
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Find all plugin tasks."),
			@ApiResponse(responseCode = "417", description = "Could not retrieve scheduled task plugins. Unexpected error occured.", 
			content = @Content(schema = @Schema(implementation = String.class))) })
	@GetMapping(value = "/plugins", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<PluginTask>> getPlugins() {
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(pluginTaskService.findAll());
	}

	@Operation(summary = "Exports filtered scheduled task list to excel.", description = "", tags = { "scheduled-task-report" })
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Exports filtered scheduled task list to excel."),
			@ApiResponse(responseCode = "400", description = "Could not create scheduled task report.Bad Request.", content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> export(@RequestParam(value = "taskCommand") Optional<String> taskCommand,
			@RequestParam(value = "startDate") @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss") Optional<Date> startDate,
			@RequestParam(value = "endDate") @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss") Optional<Date> endDate) {

		Page<CommandImpl> commands = scheduledTaskService.findAllCommandsFiltered(1, commandService.count().intValue(),
				taskCommand, startDate, endDate);
		for (CommandImpl command : commands.getContent()) {
			for (CommandExecutionImpl commandExecution : command.getCommandExecutions()) {
				for (CommandExecutionResultImpl commandExecutionResult : commandExecution
						.getCommandExecutionResults()) {
					if (commandExecutionResult.getResponseData() != null) {
						commandExecutionResult
								.setResponseDataStr((new String(commandExecutionResult.getResponseData())));
					}
				}
			}
		}

		try {

			HttpHeaders headers = new HttpHeaders();
			headers.add("fileName", "Zamanlı Çalıştırılan Görevler Raporu"
					+ new SimpleDateFormat("dd_MM_yyyy_HH:mm:ss.SSS").format(new Date()) + ".xlsx");
			headers.setContentType(MediaType.parseMediaType("application/csv"));
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
			byte[] excelContent = excelService.generateTaskReport(commands.getContent());
			return new ResponseEntity<byte[]>(excelContent, headers, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Error occured while creating excel report Error: ." + e.getMessage());
			HttpHeaders headers = new HttpHeaders();
			headers.add("message", "Error occured while creating excel report. Error: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).headers(headers).build();

		}
	}

}