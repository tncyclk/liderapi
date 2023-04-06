package tr.org.lider.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import tr.org.lider.entities.OperationType;
import tr.org.lider.entities.RoleImpl;
import tr.org.lider.ldap.LDAPServiceImpl;
import tr.org.lider.ldap.LdapEntry;
import tr.org.lider.ldap.OLCAccessRule;
import tr.org.lider.messaging.enums.DomainType;
import tr.org.lider.messaging.enums.Protocol;
import tr.org.lider.messaging.messages.XMPPClientImpl;
import tr.org.lider.models.ConfigParams;
import tr.org.lider.models.RegistrationTemplateType;
import tr.org.lider.security.CustomPasswordEncoder;
import tr.org.lider.services.AuthenticationService;
import tr.org.lider.services.ConfigurationService;
import tr.org.lider.services.OperationLogService;
import tr.org.lider.services.RoleService;
import tr.org.lider.messaging.enums.SudoRoleType;

/**
 * This controller is used for showing and updating all settings for lider
 * 
 * @author <a href="mailto:hasan.kara@pardus.org.tr">Hasan Kara</a>
 * 
 */

@Secured({"ROLE_ADMIN", "ROLE_SERVER_SETTINGS", "ROLE_CONSOLE_ACCESS_SETTINGS" })
@RestController
@RequestMapping("/api/lider/settings")
@Tag(name = "Setting Controller", description = "Settings Controller")
public class SettingsController {

	Logger logger = LoggerFactory.getLogger(SettingsController.class);

	@Autowired
	ConfigurationService configurationService;

	@Autowired
	XMPPClientImpl xmppClient;

	@Autowired
	private LDAPServiceImpl ldapService;
	
	@Autowired
	private OperationLogService operationLogService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private CustomPasswordEncoder customPasswordEncoder;
	
	@Operation(summary = "Get configurations", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Returns parameters configuration"),
			  @ApiResponse(responseCode = "417", description = "Could not get parameters configuration. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@GetMapping(value = "/configurations", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigParams>  getConfigParams() {
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(configurationService.getConfigParams());
	}

	
	@Operation(summary = "Get console users list", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Returns console users list"),
			  @ApiResponse(responseCode = "417", description = "Could not get console users list. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@GetMapping(value = "/console-users", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<LdapEntry>>  getLiderConsoleUsers() {
		List<LdapEntry> ldapEntries = null;
    	HttpHeaders headers = new HttpHeaders();
		try {
			String filter= "(&(objectClass=pardusAccount)(objectClass=pardusLider)(liderPrivilege=ROLE_USER))";
			ldapEntries  = ldapService.findSubEntries(filter,
					new String[] { "*" }, SearchScope.SUBTREE);
		} catch (LdapException e) {
			e.printStackTrace();
			return ResponseEntity
    				.status(HttpStatus.NOT_FOUND)
    				.headers(headers)
    				.build();
					
		}
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ldapEntries);
	}

	@Operation(summary = "Update ldap settings", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Updated ldap server settings"),
			  @ApiResponse(responseCode = "417", description = "Could not retrieve ldap server settings. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/update/ldap", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigParams>  updateLdapSettings(@RequestParam (value = "ldapServer", required = true) String ldapServer,
			@RequestParam (value = "ldapPort", required = true) String ldapPort,
			@RequestParam (value = "ldapUsername", required = true) String ldapUsername,
			@RequestParam (value = "ldapPassword", required = true) String ldapPassword,
			@RequestParam (value = "adIpAddress", required = true) String adIpAddress,
			@RequestParam (value = "adPort", required = true) String adPort,
			@RequestParam (value = "adDomainName", required = true) String adDomainName,
			@RequestParam (value = "adAdminUserName", required = true) String adAdminUserName,
			@RequestParam (value = "adAdminUserFullDN", required = true) String adAdminUserFullDN,
			@RequestParam (value = "adAdminPassword", required = true) String adAdminPassword,
			@RequestParam (value = "adHostName", required = true) String adHostName,
			@RequestParam (value = "adUseSSL", required = true) Boolean adUseSSL,
			@RequestParam (value = "adUseTLS", required = true) Boolean adUseTLS,
			@RequestParam (value = "adAllowSelfSignedCert", required = true) Boolean adAllowSelfSignedCert) {

		ConfigParams configParams = configurationService.getConfigParams();
		configParams.setLdapServer(ldapServer);
		configParams.setLdapPort(ldapPort);
		configParams.setLdapUsername(ldapUsername);
		configParams.setLdapPassword(ldapPassword);
		configParams.setAdIpAddress(adIpAddress);
		configParams.setAdPort(adPort);
		configParams.setAdDomainName(adDomainName);
		configParams.setAdAdminUserName(adAdminUserName);
		configParams.setAdAdminUserFullDN(adAdminUserFullDN);
		configParams.setAdAdminPassword(adAdminPassword);
		configParams.setAdHostName(adHostName);
		configParams.setAdUseSSL(adUseSSL);
		configParams.setAdUseTLS(adUseTLS);
		configParams.setAdAllowSelfSignedCert(adAllowSelfSignedCert);
		
		Map<String, Object> requestData = new HashMap<String, Object>();
		requestData.put("ldapServer",configParams.getLdapServer());
		requestData.put("ldapPort",configParams.getLdapPort());
		requestData.put("ldapUsername",configParams.getLdapUsername());
		requestData.put("adIpAddress",configParams.getAdIpAddress());
		requestData.put("adPort",configParams.getAdPort());
		requestData.put("adDomainName",configParams.getAdDomainName());
		requestData.put("adAdminUserName",configParams.getAdAdminUserName());
		requestData.put("adAdminUserFullDN",configParams.getAdAdminUserFullDN());
		requestData.put("adHostName",configParams.getAdHostName());
		requestData.put("adUseSSL",configParams.getAdUseSSL());
		requestData.put("adUseTLS",configParams.getAdUseTLS());
		requestData.put("adAllowSelfSignedCert",configParams.getAdAllowSelfSignedCert());

		ObjectMapper dataMapper = new ObjectMapper();
		String jsonString = null ;
		try {
			jsonString = dataMapper.writeValueAsString(requestData);
		} catch (JsonProcessingException e1) {
	    	HttpHeaders headers = new HttpHeaders();
			logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
			return ResponseEntity
    				.status(HttpStatus.NOT_FOUND)
    				.headers(headers)
    				.build();
		}
		String log = "LDAP server setting has been updated";
		operationLogService.saveOperationLog(OperationType.UPDATE, log, jsonString.getBytes(), null, null, null);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(configurationService.updateConfigParams(configParams));
	}

	@Operation(summary = "Update xmpp server setting", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "XMPP server setting has been updated"),
			  @ApiResponse(responseCode = "503", description = "Could not update xmpp server settings. Service Unavaible ", 
			    content = @Content(schema = @Schema(implementation = String.class))),
			  @ApiResponse(responseCode = "417", description = "Could not update xmpp server settings.Unexpected error occured", 
			    content = @Content(schema = @Schema(implementation = String.class)))
	})
	@PostMapping(value = "/update/xmpp", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigParams>  updateXMPPSettings(@RequestParam (value = "xmppHost", required = true) String xmppHost,
			@RequestParam (value = "xmppPort", required = true) int xmppPort,
			@RequestParam (value = "xmppUsername", required = true) String xmppUsername,
			@RequestParam (value = "xmppPassword", required = true) String xmppPassword,
			@RequestParam (value = "xmppMaxRetryConnectionCount", required = true) int xmppMaxRetryConnectionCount,
			@RequestParam (value = "xmppPacketReplayTimeout", required = true) int xmppPacketReplayTimeout,
			@RequestParam (value = "xmppPingTimeout", required = true) int xmppPingTimeout) {

		ConfigParams configParams = configurationService.getConfigParams();
		configParams.setXmppHost(xmppHost);
		configParams.setXmppPort(xmppPort);
		configParams.setXmppUsername(xmppUsername);
		configParams.setXmppPassword(xmppPassword);
		configParams.setXmppMaxRetryConnectionCount(xmppMaxRetryConnectionCount);
		configParams.setXmppPacketReplayTimeout(xmppPacketReplayTimeout);
		configParams.setXmppPingTimeout(xmppPingTimeout);
		ConfigParams updatedParams = configurationService.updateConfigParams(configParams);
		if(updatedParams != null) {
			logger.info("XMPP settings are updated. XMPP will disconnect and reconnect after resetting XMPP parameters.");
			try {
				xmppClient.disconnect();
				xmppClient.initXMPPClient();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error("XMPP settings are updated but error occured while connecting with new settings. Message: " + e.getMessage());
				HttpHeaders headers = new HttpHeaders();
				return ResponseEntity
	    				.status(HttpStatus.EXPECTATION_FAILED)
	    				.headers(headers)
	    				.build();
			}
		}
		
		Map<String, Object> requestData = new HashMap<String, Object>();
		requestData.put("xmppHost",configParams.getXmppHost());
		requestData.put("xmppPort",configParams.getXmppPort());
		requestData.put("xmppUsername",configParams.getXmppUsername());
		requestData.put("xmppMaxRetryConnectionCount",configParams.getXmppMaxRetryConnectionCount());
		requestData.put("xmppPacketReplayTimeout",configParams.getXmppPacketReplayTimeout());
		requestData.put("xmppPingTimeout",configParams.getXmppPingTimeout());

		ObjectMapper dataMapper = new ObjectMapper();
		String jsonString = null ;
		try {
			jsonString = dataMapper.writeValueAsString(requestData);
		} catch (JsonProcessingException e1) {
			logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
			HttpHeaders headers = new HttpHeaders();
			return ResponseEntity
    				.status(HttpStatus.EXPECTATION_FAILED)
    				.headers(headers)
    				.build();
		}
		String log = "XMPP server setting has been updated";
		operationLogService.saveOperationLog(OperationType.UPDATE, log, jsonString.getBytes(), null, null, null);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(updatedParams);
				
	}

	@Operation(summary = "Update file server ", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "File server settings updated."),
			  @ApiResponse(responseCode = "417", description = "Could not update file server settings. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/update/file-server", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigParams>  updateFileServerSettings(@RequestParam (value = "fileTransferType", required = true) Protocol fileTransferType,
			@RequestParam (value = "fileServerAddress", required = true) String fileServerAddress,
			@RequestParam (value = "fileServerUsername", required = true) String fileServerUsername,
			@RequestParam (value = "fileServerPassword", required = true) String fileServerPassword,
			@RequestParam (value = "fileServerPort", required = true) int fileServerPort,
			@RequestParam (value = "fileServerAgentFilePath", required = true) String fileServerAgentFilePath) {

		ConfigParams configParams = configurationService.getConfigParams();
		configParams.setFileServerProtocol(fileTransferType);
		configParams.setFileServerPort(fileServerPort);
		configParams.setFileServerHost(fileServerAddress);
		configParams.setFileServerUsername(fileServerUsername);
		configParams.setFileServerPassword(fileServerPassword);
		configParams.setFileServerAgentFilePath(fileServerAgentFilePath);
		
		Map<String, Object> requestData = new HashMap<String, Object>();
		requestData.put("fileServerAddress",configParams.getFileServerHost());
		requestData.put("fileServerUsername",configParams.getFileServerUsername());
		requestData.put("fileServerPort",configParams.getFileServerPort());
		requestData.put("fileServerAgentFilePath",configParams.getFileServerAgentFilePath());
		requestData.put("fileTransferType",configParams.getFileServerProtocol());

		ObjectMapper dataMapper = new ObjectMapper();
		String jsonString = null ;
		try {
			jsonString = dataMapper.writeValueAsString(requestData);
		} catch (JsonProcessingException e1) {
			logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
			HttpHeaders headers = new HttpHeaders();
			return ResponseEntity
    				.status(HttpStatus.EXPECTATION_FAILED)
    				.headers(headers)
    				.build();
		}
		String log = "File server setting has been updated";
		operationLogService.saveOperationLog(OperationType.UPDATE, log, jsonString.getBytes(), null, null, null);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(configurationService.updateConfigParams(configParams));
	}

	@Operation(summary = "Update email settings", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Email settings updated."),
			  @ApiResponse(responseCode = "417", description = "could not update email settings. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/update/email-settings", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigParams>  updateEmailSettings(
			@RequestParam (value = "emailHost", required = false) String emailHost,
			@RequestParam (value = "emailPort", required = false) Integer emailPort,
			@RequestParam (value = "emailUsername", required = false) String emailUsername,
			@RequestParam (value = "emailPassword", required = false) String emailPassword,
			@RequestParam (value = "smtpAuth", required = false) Boolean smtpAuth,
			@RequestParam (value = "tlsEnabled", required = false) Boolean tlsEnabled) {
		
		ConfigParams configParams = configurationService.getConfigParams();
		configParams.setMailHost(emailHost);
		configParams.setMailPassword(emailPassword);
		configParams.setMailSmtpPort(emailPort);
		configParams.setMailSmtpAuth(smtpAuth);
		configParams.setMailSmtpStartTlsEnable(tlsEnabled);
		configParams.setMailAddress(emailUsername);

		Map<String, Object> requestData = new HashMap<String, Object>();
		requestData.put("emailHost",configParams.getMailHost());
		requestData.put("emailPort",configParams.getMailPassword());
		requestData.put("emailUsername",configParams.getMailSmtpPort());
		requestData.put("smtpAuth",configParams.getMailSmtpStartTlsEnable());
		requestData.put("tlsEnabled",configParams.getMailAddress());

		ObjectMapper dataMapper = new ObjectMapper();
		String jsonString = null ;
		try {
			jsonString = dataMapper.writeValueAsString(requestData);
		} catch (JsonProcessingException e1) {
			logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
			HttpHeaders headers = new HttpHeaders();
			return ResponseEntity
    				.status(HttpStatus.EXPECTATION_FAILED)
    				.headers(headers)
    				.build();
		}
		String log = "Mail server setting has been updated";
		operationLogService.saveOperationLog(OperationType.UPDATE, log, jsonString.getBytes(), null, null, null);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(configurationService.updateConfigParams(configParams));
				
	}
	
	@Operation(summary = "Update other settings", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Updated other setting"),
			  @ApiResponse(responseCode = "417", description = "Could not update other settings. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/update/other-settings", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigParams>  updateOtherSettings(@RequestParam (value = "disableLocalUser", required = true) Boolean disableLocalUser,
			@RequestParam (value = "domainType", required = true) DomainType domainType,
			@RequestParam (value = "ahenkRepoAddress", required = true) String ahenkRepoAddress,
			@RequestParam (value = "ahenkRepoKeyAddress", required = true) String ahenkRepoKeyAddress,
			@RequestParam (value = "sudoRoleType", required = true) SudoRoleType sudoRoleType,
			@RequestParam (value = "selectedRegistrationType", required = true) RegistrationTemplateType selectedRegistrationType) {
		
		ConfigParams configParams = configurationService.getConfigParams();
		configParams.setDisableLocalUser(disableLocalUser);
		configParams.setDomainType(domainType);
		configParams.setsudoRoleType(sudoRoleType);
		configParams.setAhenkRepoAddress(ahenkRepoAddress);
		configParams.setAhenkRepoKeyAddress(ahenkRepoKeyAddress);
		configParams.setSelectedRegistrationType(selectedRegistrationType);
		
		Map<String, Object> requestData = new HashMap<String, Object>();
		requestData.put("domainType",configParams.getDomainType());
		requestData.put("ahenkRepoAddress",configParams.getAhenkRepoAddress());
		requestData.put("ahenkRepoKeyAddress",configParams.getAhenkRepoKeyAddress());
		requestData.put("sudoRoleType",configParams.getSudoRoleType());
		requestData.put("selectedRegistrationType",configParams.getSelectedRegistrationType());
		requestData.put("disableLocalUser",configParams.getDisableLocalUser());

		ObjectMapper dataMapper = new ObjectMapper();
		String jsonString = null ;
    	HttpHeaders headers = new HttpHeaders();
		try {
			jsonString = dataMapper.writeValueAsString(requestData);
		} catch (JsonProcessingException e1) {
			logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
			return ResponseEntity
					.status(HttpStatus.EXPECTATION_FAILED)
					.headers(headers)
					.build();
		}
		String log = "Other server setting has been updated";
		operationLogService.saveOperationLog(OperationType.UPDATE, log, jsonString.getBytes(), null, null, null);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(configurationService.updateConfigParams(configParams));
				
	}

	//add roles to user. 
	@Operation(summary = "Edit user roles in ldap entries", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Edited user roles in ldap entries"),
			  @ApiResponse(responseCode = "417", description = "Could not edit user roles in ldap entries. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/edit-user-roles", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<LdapEntry>> editUserRoles(
			@RequestParam (value = "dn", required = true) String dn,
			@RequestParam(value = "roles[]", required=true) String[] roles,
			Authentication authentication) {
		List<LdapEntry> ldapEntries = null;
		try {
			LdapEntry entry = ldapService.getEntryDetail(dn);
			if(entry != null) {
				if(entry.getAttributesMultiValues().get("liderPrivilege") != null) {
					String[] priviliges = entry.getAttributesMultiValues().get("liderPrivilege");
					for (int i = 0; i < priviliges.length; i++) {
						if(priviliges[i].startsWith("ROLE_")) {
							ldapService.updateEntryRemoveAttribute(dn, "liderPrivilege");
						}
					}
					for (int i = 0; i < roles.length; i++) {
						ldapService.updateEntryAddAtribute(dn, "liderPrivilege", roles[i]);
					}
				} else {
					for (int i = 0; i < roles.length; i++) {
						ldapService.updateEntryAddAtribute(dn, "liderPrivilege", roles[i]);
					}
				}
			}
			//if user edited own console roles redirect to logout
			if(AuthenticationService.getDn().equals(dn)) {
				authentication.setAuthenticated(false);
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			} else {
				String filter= "(&(objectClass=pardusAccount)(objectClass=pardusLider)(liderPrivilege=ROLE_USER))";
				ldapEntries  = ldapService.findSubEntries(filter,
						new String[] { "*" }, SearchScope.SUBTREE);
				try {
					Map<String, Object> requestData = new HashMap<String, Object>();
					requestData.put("dn", entry.getDistinguishedName());
					requestData.put("liderPrivilege", roles);
					ObjectMapper dataMapper = new ObjectMapper();
					String jsonString = dataMapper.writeValueAsString(requestData);
					String log = entry.getDistinguishedName()+ " Lider Privileges has been changed";
					operationLogService.saveOperationLog(OperationType.UPDATE, log, jsonString.getBytes(), null, null, null);
				}catch (Exception e) {
					logger.error("Error occured while mapping request data to json. Error: " +  e.getMessage());
					HttpHeaders headers = new HttpHeaders();
					return ResponseEntity							
							.status(HttpStatus.EXPECTATION_FAILED)
							.headers(headers)
							.build();
				}
				return new ResponseEntity<>(ldapEntries, HttpStatus.OK);
			}
		} catch (LdapException e) {
			e.printStackTrace();
			HttpHeaders headers = new HttpHeaders();
			return ResponseEntity							
					.status(HttpStatus.EXPECTATION_FAILED)
					.headers(headers)
					.build();
		}
	}

	@Operation(summary = "Delete console user in ldap entry", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Console user deleted on ldap entries"),
			  @ApiResponse(responseCode = "417", description = "Could not delete console user in ldap entries. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@DeleteMapping(value = "/delete-console-user/dn/{dn}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<LdapEntry>> deleteConsoleUser(@PathVariable String dn,
			Authentication authentication) {
		List<LdapEntry> ldapEntries = null;
		try {
			LdapEntry entry = ldapService.getEntryDetail(dn);
			if(entry != null) {
				if(entry.getAttributesMultiValues().get("liderPrivilege") != null) {
					String[] priviliges = entry.getAttributesMultiValues().get("liderPrivilege");
					for (int i = 0; i < priviliges.length; i++) {
						if(priviliges[i].startsWith("ROLE_")) {
							ldapService.updateEntryRemoveAttributeWithValue(dn, "liderPrivilege", priviliges[i]);
						}
					}
				}
			}
			//if user deleted own console roles redirect to logout
			if(AuthenticationService.getDn().equals(dn)) {
				authentication.setAuthenticated(false);
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			} else {
				String filter= "(&(objectClass=pardusAccount)(objectClass=pardusLider)(liderPrivilege=ROLE_USER))";
				ldapEntries  = ldapService.findSubEntries(filter,
						new String[] { "*" }, SearchScope.SUBTREE);
				

				Map<String, Object> requestData = new HashMap<String, Object>();	
				requestData.put("dn",entry.getDistinguishedName());
				requestData.put("menuRole",entry.getAttributesMultiValues().get("liderPrivilege"));
				ObjectMapper dataMapper = new ObjectMapper();
				String jsonString = null ;
				try {
					jsonString = dataMapper.writeValueAsString(requestData);
				} catch (JsonProcessingException e1) {
					logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
				}
				String log = entry.getName() + " Lider privileges access has been deleted" ;
				operationLogService.saveOperationLog(OperationType.DELETE, log, jsonString.getBytes(), null, null, null);
				
				return new ResponseEntity<>(ldapEntries, HttpStatus.OK);
			}

		} catch (LdapException e) {
			e.printStackTrace();
			HttpHeaders headers = new HttpHeaders();
			return ResponseEntity							
					.status(HttpStatus.EXPECTATION_FAILED)
					.headers(headers)
					.build();
		}
	}

	@Operation(summary = "Get user roles", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Returns user roles"),
			  @ApiResponse(responseCode = "400", description = "Could not retrieve user roles. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@GetMapping(value = "/roles", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<RoleImpl>>  getRoles() {
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(roleService.getRoles());
				
	}

	@Operation(summary = "Create role for menu setting", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Created role"),
			  @ApiResponse(responseCode = "417", description = "Could not create role. Unexpected error occured.", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/save-menus-for-role", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<RoleImpl>> saveMenusForRole(@RequestBody RoleImpl role) {
		if(!role.getName().equals("ROLE_ADMIN")) {
			roleService.saveRole(role);
			
			Map<String, Object> requestData = new HashMap<String, Object>();
			requestData.put("menuRole",role);
			ObjectMapper dataMapper = new ObjectMapper();
			String jsonString = null ;
			try {
				jsonString = dataMapper.writeValueAsString(requestData);
			} catch (JsonProcessingException e1) {
				logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
			}
			String log = role.getName() + " Menu settings has been chancged" ;
			operationLogService.saveOperationLog(OperationType.UPDATE, log, jsonString.getBytes(), null, null, null);
			
			return ResponseEntity
					.status(HttpStatus.OK)
					.body(roleService.getRoles());
					
		} else {
			HttpHeaders headers = new HttpHeaders();
			return ResponseEntity							
					.status(HttpStatus.EXPECTATION_FAILED)
					.headers(headers)
					.build();
		}
	}

	@Operation(summary = "Get access ldap rules", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Get open ldap check rule list"),
			  @ApiResponse(responseCode = "417", description = "Could not retrieved open ldap  check rule list. Unexpected error occured", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@GetMapping(value = "/OLC-access-rules/dn/{dn}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<OLCAccessRule>> getUsersOLCAccessRules(@PathVariable String dn) {
		HttpHeaders headers = new HttpHeaders();
		if(!dn.equals("")) {
			try {
				List<OLCAccessRule> ruleList = ldapService.getSubTreeOLCAccessRules(dn);
				return ResponseEntity
						.status(HttpStatus.OK)
						.body(ruleList);
						
			} catch (LdapException e) {
				logger.error(e.getMessage());
				return ResponseEntity							
						.status(HttpStatus.EXPECTATION_FAILED)
						.headers(headers)
						.build();
			}
		} else {
			return ResponseEntity							
					.status(HttpStatus.EXPECTATION_FAILED)
					.headers(headers)
					.build();
		}
	}

	@Operation(summary = "Add new access rule ", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Created new access rule"),
			  @ApiResponse(responseCode = "417", description = "Could not add access rule. Unexpected errror occured", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/add-OLC-access-rule", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Boolean>  addOLCAccessRule(
			@RequestParam (value = "groupDN", required = true) String groupDN,
			@RequestParam (value = "olcAccessDN", required = true) String olcAccessDN,
			@RequestParam (value = "accessType", required = true) String accessType) {
		if(!groupDN.equals("")) {
			OLCAccessRule rule = new OLCAccessRule();
			rule.setAccessDNType("dn.subtree");
			rule.setAccessDN(olcAccessDN);
			rule.setAssignedDNType("group.exact");
			rule.setAssignedDN(groupDN);
			rule.setAccessType(accessType);
			
			Map<String, Object> requestData = new HashMap<String, Object>();
			requestData.put("olcAccess",rule);
			ObjectMapper dataMapper = new ObjectMapper();
			String jsonString = null ;
			try {
				jsonString = dataMapper.writeValueAsString(requestData);
			} catch (JsonProcessingException e1) {
				logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
			}
			String log = rule.getAssignedDN() + " OLC Access has been added" ;
			operationLogService.saveOperationLog(OperationType.CREATE, log, jsonString.getBytes(), null, null, null);
			
			return ResponseEntity
					.status(HttpStatus.OK)
					.body(ldapService.addOLCAccessRule(rule));
	
		} else {
			return ResponseEntity
					.status(HttpStatus.EXPECTATION_FAILED)
					.body(false);
					
		}
	}
	

	@Operation(summary = "Delete access rule", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Access rule deleted"),
			  @ApiResponse(responseCode = "417", description = "Could not delete access rule. Unexpected errror occured", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/delete-OLC-access-rule", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Boolean>  deleteOLCAccessRule(@RequestBody OLCAccessRule rule) 
	{
		Map<String, Object> requestData = new HashMap<String, Object>();
		requestData.put("olcAccess",rule);
		ObjectMapper dataMapper = new ObjectMapper();
		String jsonString = null ;
		try {
			jsonString = dataMapper.writeValueAsString(requestData);
		} catch (JsonProcessingException e1) {
			logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
			HttpHeaders headers = new HttpHeaders();
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).headers(headers).build();
		}
		String log = rule.getAssignedDN() + " OLC Access has been deleted" ;
		operationLogService.saveOperationLog(OperationType.DELETE, log, jsonString.getBytes(), null, null, null);
		
		ldapService.removeOLCAccessRuleWithParents(rule);
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(true);
	}

	/**
	 * 
	 * add console user from settings
	 * edip.yildiz
	 * @param selectedEntry from 
	 * @return
	 */
	@Operation(summary = "Add console user", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Added console user for ldap entries"),
			  @ApiResponse(responseCode = "417", description = "Could not add console user for ldap entries. Unexpected error occured", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/add-console-user-btn",produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<LdapEntry>  addConsoleUserBtn(LdapEntry user) {
		try {
			String gidNumber="6000";
			int randomInt = (int)(1000000.0 * Math.random());
			String uidNumber= Integer.toString(randomInt);
			String home="/home/"+user.getUid();

			Map<String, String[]> attributes = new HashMap<String, String[]>();
			attributes.put("objectClass", new String[] { "top", "posixAccount",
					"person","pardusLider","pardusAccount","organizationalPerson","inetOrgPerson"});
			attributes.put("cn", new String[] { user.getCn() });
			attributes.put("mail", new String[] { user.getMail() });
			attributes.put("gidNumber", new String[] { gidNumber });
			attributes.put("homeDirectory", new String[] { home });
			attributes.put("sn", new String[] { user.getSn() });
			attributes.put("uid", new String[] { user.getUid() });
			attributes.put("uidNumber", new String[] { uidNumber });
			attributes.put("loginShell", new String[] { "/bin/bash" });
			attributes.put("userPassword", new String[] { "{ARGON2}" + customPasswordEncoder.encode(user.getUserPassword()) });
			attributes.put("homePostalAddress", new String[] { user.getHomePostalAddress() });
			if(user.getTelephoneNumber()!=null && user.getTelephoneNumber()!="")
				attributes.put("telephoneNumber", new String[] { user.getTelephoneNumber() });

			if(user.getParentName()==null || user.getParentName().equals("")) {
				user.setParentName(configurationService.getUserLdapBaseDn());
			}
			
			String rdn="uid="+user.getUid()+","+user.getParentName();

			ldapService.addEntry(rdn, attributes);
			
			user.setAttributesMultiValues(attributes);
			user.setDistinguishedName(user.getUid());

			logger.info("User created successfully RDN ="+rdn);
			user = ldapService.findSubEntries(rdn, "(objectclass=*)", new String[] {"*"}, SearchScope.OBJECT).get(0);
			
			Map<String, Object> requestData = new HashMap<String, Object>();
			requestData.put("dn",user.getDistinguishedName());
			ObjectMapper dataMapper = new ObjectMapper();
			String jsonString = null ;
			try {
				jsonString = dataMapper.writeValueAsString(requestData);
			} catch (JsonProcessingException e1) {
				logger.error("Error occured while mapping request data to json. Error: " +  e1.getMessage());
			}
			String log = user.getDistinguishedName() + " Console user has been added";
			operationLogService.saveOperationLog(OperationType.CREATE, log, jsonString.getBytes(), null, null, null);
			
			return ResponseEntity
					.status(HttpStatus.OK)
					.body(user);
					
		} catch (LdapException e) {
			HttpHeaders headers = new HttpHeaders();
			return ResponseEntity							
					.status(HttpStatus.EXPECTATION_FAILED)
					.headers(headers)
					.build();
		}
	}

	
	@Operation(summary = "Member added to the group ", description = "", tags = { "settings" })
	@ApiResponses(value = { 
			  @ApiResponse(responseCode = "200", description = "Member has been added to the group at the ldap entry"),
			  @ApiResponse(responseCode = "417", description = "Could not  add members to the group in ldap entry. Unexpected error occured", 
			    content = @Content(schema = @Schema(implementation = String.class))) })
	@PostMapping(value = "/add-member-to-group")
	public ResponseEntity<Boolean>  addMemberToGroup(HttpServletRequest request, LdapEntry selectedEntry) {
		logger.info("Adding {} to group. Group info {} ", selectedEntry.getDistinguishedName(),selectedEntry.getParentName());
		try {
			ldapService.updateEntryAddAtribute(selectedEntry.getParentName(), "member", selectedEntry.getDistinguishedName());
			operationLogService.saveOperationLog(OperationType.CREATE,"Gruba üye eklendi. Üye: "+selectedEntry.getDistinguishedName(),null);
		} catch (LdapException e) {
			e.printStackTrace();
			HttpHeaders headers = new HttpHeaders();
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).headers(headers).build();
		}
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(true);
		
	}
	
}