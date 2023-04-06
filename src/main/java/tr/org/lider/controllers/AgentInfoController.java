package tr.org.lider.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.PathVariable;
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
import tr.org.lider.entities.AgentImpl;
import tr.org.lider.services.AgentService;
import tr.org.lider.services.ExcelExportService;

@Secured({"ROLE_ADMIN", "ROLE_AGENT_INFO" })
@RestController
@RequestMapping("/api/lider/agent-info")
@Tag(name = "Agent service", description = "Agent controller")
public class AgentInfoController {

	private final Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private AgentService agentService;

	@Autowired
	private ExcelExportService excelService;

	@Operation(summary = "Find all agents", description = "", tags = { "agent-service" })
	@ApiResponses(value = { 
			@ApiResponse(responseCode = "200", description = "Find all agents", 
				content = { @Content(schema = @Schema(implementation = AgentImpl.class)) 
				}), 
			@ApiResponse(responseCode = "417", description = "Could not retrieve agents list. Unexpected error occured.", 
			content = { @Content(schema = @Schema(implementation = String.class)) 
			}) 
		})
	@PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<HashMap<String, Object>> findAllAgents(
			@RequestParam (value = "pageNumber") int pageNumber,
			@RequestParam (value = "pageSize") int pageSize,
			@RequestParam (value = "sessionReportType") Optional<String> sessionReportType,
			@RequestParam (value = "getFilterData") Optional<Boolean> getFilterData,
			@RequestParam (value = "registrationStartDate") @DateTimeFormat(pattern="dd/MM/yyyy HH:mm:ss") Optional<Date> registrationStartDate,
			@RequestParam (value = "registrationEndDate") @DateTimeFormat(pattern="dd/MM/yyyy HH:mm:ss") Optional<Date> registrationEndDate,
			@RequestParam (value = "status") Optional<String> status,
			@RequestParam (value = "dn") Optional<String> dn,
			@RequestParam (value = "hostname") Optional<String> hostname,
			@RequestParam (value = "macAddress") Optional<String> macAddress,
			@RequestParam (value = "ipAddress") Optional<String> ipAddress,
			@RequestParam (value = "brand") Optional<String> brand,
			@RequestParam (value = "model") Optional<String> model,
			@RequestParam (value = "processor") Optional<String> processor,
			@RequestParam (value = "osVersion") Optional<String> osVersion,
			@RequestParam (value = "agentVersion") Optional<String> agentVersion,
			@RequestParam (value = "diskType") Optional<String> diskType) {
		HashMap<String, Object> resultMap = new HashMap<>();
		if(getFilterData.isPresent() && getFilterData.get()) {
			resultMap.put("brands", agentService.getBrands());
			resultMap.put("models", agentService.getmodels());
			resultMap.put("processors", agentService.getProcessors());
			resultMap.put("osVersions", agentService.getOSVersions());
			resultMap.put("agentVersions", agentService.getAgentVersions());
			resultMap.put("diskType", agentService.getDiskType());
		}
		Page<AgentImpl> listOfAgents = agentService.findAllAgents(
				pageNumber, 
				pageSize, 
				sessionReportType,
				registrationStartDate, 
				registrationEndDate, 
				status, 
				dn,
				hostname, 
				macAddress, 
				ipAddress, 
				brand, 
				model, 
				processor, 
				osVersion,
				agentVersion,
				diskType);
				
		resultMap.put("agents", listOfAgents);
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(resultMap);
	}

	//get agent detail by ID
	@Operation(summary = "Find agent detail by id.", description = "", tags = { "agent-service" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Find agent detail by id.", 
			    content = { @Content(schema = @Schema(implementation = AgentImpl.class)) }),
			  @ApiResponse(responseCode = "404", description = "Agent id not found.Not found.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@GetMapping(value = "/detail/id/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AgentImpl>  findAgentByIDRest(@PathVariable Long agentID) {
		logger.debug("Agent id:  {} ", agentID);
		Optional<AgentImpl> agent = agentService.findAgentByID(agentID);
		HttpHeaders headers = new HttpHeaders();
		if(agent.isPresent()) {
			return new ResponseEntity<AgentImpl>(agent.get(), HttpStatus.OK);
		}
		else {
			return ResponseEntity
    				.status(HttpStatus.NOT_FOUND)
    				.headers(headers)
    				.build();
		}
	}

	@Operation(summary = "Exports filtered agent list to excel", description = "", tags = { "agent-service" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Created excel file successfully", 
			    content = { @Content(schema = @Schema(implementation = AgentImpl.class)) }),
			  @ApiResponse(responseCode = "400", description = "Could not create client report.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> export(
			@RequestParam (value = "registrationStartDate") @DateTimeFormat(pattern="dd/MM/yyyy HH:mm:ss") Optional<Date> registrationStartDate,
			@RequestParam (value = "registrationEndDate") @DateTimeFormat(pattern="dd/MM/yyyy HH:mm:ss") Optional<Date> registrationEndDate,
			@RequestParam (value = "status") Optional<String> status,
			@RequestParam (value = "sessionReportType") Optional<String> sessionReportType,
			@RequestParam (value = "dn") Optional<String> dn,
			@RequestParam (value = "hostname") Optional<String> hostname,
			@RequestParam (value = "macAddress") Optional<String> macAddress,
			@RequestParam (value = "ipAddress") Optional<String> ipAddress,
			@RequestParam (value = "brand") Optional<String> brand,
			@RequestParam (value = "model") Optional<String> model,
			@RequestParam (value = "processor") Optional<String> processor,
			@RequestParam (value = "osVersion") Optional<String> osVersion,
			@RequestParam (value = "agentVersion") Optional<String> agentVersion,
			@RequestParam (value = "diskType") Optional<String> diskType){
		Page<AgentImpl> listOfAgents = agentService.findAllAgents(
				1, 
				agentService.count().intValue(), 
				sessionReportType,
				registrationStartDate, 
				registrationEndDate, 
				status, 
				dn,
				hostname, 
				macAddress, 
				ipAddress, 
				brand, 
				model, 
				processor, 
				osVersion, 
				agentVersion,
				diskType);
		
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("fileName", "Istemci Raporu_" + new SimpleDateFormat("dd_MM_yyyy_HH:mm:ss.SSS").format(new Date()) + ".xlsx");
			headers.setContentType(MediaType.parseMediaType("application/csv"));
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
			byte[] excelContent = excelService.generateAgentReport(listOfAgents.getContent());
			return new ResponseEntity<byte[]>(excelContent, headers,  HttpStatus.OK);
		} catch (Exception e) {
        	logger.error("Error occured while creating excel report Error: ." + e.getMessage());
        	HttpHeaders headers = new HttpHeaders();
        	headers.add("message", "Error occured while creating excel report. Error: " + e.getMessage());
    		return ResponseEntity
    				.status(HttpStatus.EXPECTATION_FAILED)
    				.headers(headers)
    				.build();
		}
		
		
	}
}