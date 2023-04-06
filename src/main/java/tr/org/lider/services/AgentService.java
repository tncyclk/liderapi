package tr.org.lider.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import tr.org.lider.entities.AgentImpl;
import tr.org.lider.ldap.LDAPServiceImpl;
import tr.org.lider.ldap.LdapEntry;
import tr.org.lider.messaging.messages.XMPPClientImpl;
import tr.org.lider.repositories.AgentInfoCriteriaBuilder;
import tr.org.lider.repositories.AgentRepository;

@Service
public class AgentService {

	@Autowired
	private AgentRepository agentRepository;
	
	@Autowired
	private XMPPClientImpl messagingService;
	
	@Autowired
	private AgentInfoCriteriaBuilder agentInfoCB;
	
	@Autowired
	private ConfigurationService configurationService;
	
	@Autowired
	private LDAPServiceImpl ldapService;
	
	public List<AgentImpl> findAll() {
        return agentRepository.findAll();
	}
	
	public Long count() {
        return agentRepository.count();
	}
	
	public Optional<AgentImpl> findAgentByID(Long agentID) {
        return agentRepository.findById(agentID);
	}
	
	public List<AgentImpl> findAgentByJid(String agentJid) {
        return agentRepository.findByJid(agentJid);
	}
	
	public List<AgentImpl> findAgentByHostname(String hostname) {
        return agentRepository.findByHostname(hostname);
	}
	
	public List<AgentImpl> findAgentByDn(String agentDn) {
        return agentRepository.findByDn(agentDn);
	}
	
	public AgentImpl updateUserDirectoryAgentByJid(String jid, String userDirectoryDomain) {
		List<AgentImpl> existAgent = agentRepository.findByJid(jid);
		if(existAgent != null && existAgent.size() > 0) {
			existAgent.get(0).setUserDirectoryDomain(userDirectoryDomain);
			return agentRepository.save(existAgent.get(0));
		} else {
			return null;
		}
	}
	
	public AgentImpl updateUserDirectoryAgentByDn(String dn, String userDirectoryDomain) {
		List<AgentImpl> existAgent = agentRepository.findByDn(dn);
		if(existAgent != null && existAgent.size() > 0) {
			existAgent.get(0).setUserDirectoryDomain(userDirectoryDomain);
			return agentRepository.save(existAgent.get(0));
		} else {
			return null;
		}
	}
	
	public Page<AgentImpl> findAllAgents(
			int pageNumber,
			int pageSize,
			Optional<String> sessionReportType,
			Optional<Date> registrationStartDate,
			Optional<Date> registrationEndDate,
			Optional<String> status,
			Optional<String> dn,
			Optional<String> hostname,
			Optional<String> macAddress,
			Optional<String> ipAddress,
			Optional<String> brand,
			Optional<String> model,
			Optional<String> processor,
			Optional<String> osVersion,
			Optional<String> agentVersion,
			Optional<String> diskType) {
		
		List<String> listOfOnlineUsers = new ArrayList<String>();
		if(!status.get().equals("ALL")) {
			
			List<LdapEntry> listOfAgents = new ArrayList<LdapEntry>();
			try {
	
				listOfAgents = ldapService.findSubEntries(
						configurationService.getAgentLdapBaseDn(), "(objectclass=pardusDevice)", new String[] { "*" }, SearchScope.SUBTREE);
	
				for (LdapEntry ldapEntry : listOfAgents) {
					if(ldapEntry.isOnline()) {
						listOfOnlineUsers.add(ldapEntry.getUid());
					}
				}
			} catch (LdapException e) {
				e.printStackTrace();
			}
		}
		if(sessionReportType.isPresent()) {
			if(sessionReportType.get().equals("LAST_ONE_MONTH_NO_SESSIONS") 
					|| sessionReportType.get().equals("LAST_TWO_MONTHS_NO_SESSIONS") 
					|| sessionReportType.get().equals("LAST_THREE_MONTHS_NO_SESSIONS")) {
				List<LdapEntry> listOfAgents = new ArrayList<LdapEntry>();
				try {
		
					listOfAgents = ldapService.findSubEntries(
							configurationService.getAgentLdapBaseDn(), "(objectclass=pardusDevice)", new String[] { "*" }, SearchScope.SUBTREE);
		
					for (LdapEntry ldapEntry : listOfAgents) {
						if(ldapEntry.isOnline()) {
							listOfOnlineUsers.add(ldapEntry.getUid());
						}
					}
				} catch (LdapException e) {
					e.printStackTrace();
				}
			}
		}
		Page<AgentImpl> listOfAgentsCB = agentInfoCB.filterAgents(
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
				diskType,
				listOfOnlineUsers
				);
		for (int i = 0; i < listOfAgentsCB.getContent().size(); i++) {
			if(messagingService.isRecipientOnline(listOfAgentsCB.getContent().get(i).getJid())) {
				listOfAgentsCB.getContent().get(i).setIsOnline(true);
			}
			else {
				listOfAgentsCB.getContent().get(i).setIsOnline(false);
			}
		}
		return listOfAgentsCB;
	}
	
	public void updateAgentDN(String currentDN, String newDN) {
		agentRepository.updateAgentDN(currentDN, newDN);
	}
	
	public void updateHostname(String currentDN, String newDN, String newHostname) {
		agentRepository.updateHostname(currentDN, newDN, newHostname);
	}
	
	public void deleteAgent(String dn) {
		List<AgentImpl> agentList = agentRepository.findByDn(dn);
		agentRepository.deleteById(agentList.get(0).getId());
	}
	
	public List<String> getBrands() {
		return agentRepository.getPropertyValueByName("hardware.baseboard.manufacturer");
	}
	
	public List<String> getmodels() {
		return agentRepository.getPropertyValueByName("hardware.baseboard.productName");
	}
	
	public List<String> getProcessors() {
		return agentRepository.getPropertyValueByName("processor");
	}
	
	public List<String> getOSVersions() {
		return agentRepository.getPropertyValueByName("os.version");
	}
	
	public List<String> getAgentVersions() {
		return agentRepository.getPropertyValueByName("agentVersion");
	}
	
	public int getCountByCreateDate(Date startDate, Date endDate) {
		return agentRepository.getCountByCreateDate(startDate, endDate);
	}
	
	public int getCountByTodayCreateDate(Date startDate) {
		return agentRepository.getCountByTodayCreateDate(startDate);
	}
	
	public int getCountByTodayLastLogin(Date startDate) {
		return agentRepository.getCountByLastLoginToday(startDate);
	}
	
	public List<String> getDiskType() {
		return agentRepository.getPropertyValueByName("diskType");
	}
	
	
}
