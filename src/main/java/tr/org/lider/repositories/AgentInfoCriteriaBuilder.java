package tr.org.lider.repositories;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import tr.org.lider.entities.AgentImpl;
import tr.org.lider.entities.AgentPropertyImpl;

/*
 * AgentInfoCriteriaBuilder implements filtering agents with multiple data.
 * Filter can be applied over status(online, offline) registration date and agent properties
 * 
 * @author <a href="mailto:hasan.kara@pardus.org.tr">Hasan Kara</a>
 */

@Service
public class AgentInfoCriteriaBuilder {

	@PersistenceContext
	private EntityManager entityManager;


	public Page<AgentImpl> filterAgents(
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
			Optional<String> diskType,
			List<String> listOfOnlineUsers) {
		PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize);

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		//for filtered result count
		CriteriaBuilder cbCount = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaCount = cbCount.createQuery(Long.class);
		Root<AgentImpl> fromCount = criteriaCount.from(AgentImpl.class);
		criteriaCount.select(cbCount.count(fromCount));

		CriteriaQuery<AgentImpl> criteriaQuery = cb.createQuery(AgentImpl.class);
		Root<AgentImpl> from = criteriaQuery.from(AgentImpl.class);
		CriteriaQuery<AgentImpl> select = criteriaQuery.select(from);

		List<Predicate> predicates = new ArrayList<>();
		//for filtered result count
		List<Predicate> predicatesCount = new ArrayList<>();


		if(dn.isPresent() && !dn.get().equals("")) {
			predicates.add(cb.like(from.get("dn").as(String.class), "%" + dn.get() + "%"));
			predicatesCount.add(cbCount.like(fromCount.get("dn").as(String.class), "%" + dn.get() + "%"));
		}

		if (registrationStartDate.isPresent()) {
			predicates.add(cb.greaterThanOrEqualTo(from.get("createDate"), registrationStartDate.get()));
			predicatesCount.add(cbCount.greaterThanOrEqualTo(fromCount.get("createDate"), registrationStartDate.get()));
		}
		if (registrationEndDate.isPresent()) {
			predicates.add(cb.lessThanOrEqualTo(from.get("createDate"), registrationEndDate.get()));
			predicatesCount.add(cbCount.lessThanOrEqualTo(fromCount.get("createDate"), registrationEndDate.get()));
		}

		if(status.get().equals("ONLINE") ) {
			predicates.add(cb.in(from.get("jid"))
					.value(!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList(""))));
			predicatesCount.add(cbCount.in(fromCount.get("jid"))
					.value(!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList(""))));
		} else if(status.get().equals("OFFLINE")) {
			predicates.add(
					cb.not(from.get("jid").in(
							!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList("")))));
			predicatesCount.add(
					cbCount.not(fromCount.get("jid").in(
							!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList("")))));
		}

		if(hostname.isPresent() && !hostname.get().equals("")) {
			predicates.add(cb.like(from.get("hostname").as(String.class), "%" + hostname.get() + "%"));
			predicatesCount.add(cbCount.like(fromCount.get("hostname").as(String.class), "%" + hostname.get() + "%") );
		}
		
		if(macAddress.isPresent() && !macAddress.get().equals("")) {
			predicates.add(cb.like(from.get("macAddresses").as(String.class), "%" + macAddress.get() + "%"));
			predicatesCount.add(cbCount.like(fromCount.get("macAddresses").as(String.class), "%" + macAddress.get() + "%") );
		}
		
		if(ipAddress.isPresent() && !ipAddress.get().equals("")) {
			predicates.add(cb.like(from.get("ipAddresses").as(String.class), "%" + ipAddress.get() + "%"));
			predicatesCount.add(cbCount.like(fromCount.get("ipAddresses").as(String.class), "%" + ipAddress.get() + "%") );
		}
		
		if(brand.isPresent() && !brand.get().equals("")) {
			Join<AgentImpl, AgentPropertyImpl> properties = from.join("properties");
			Predicate namePredicate = cb.like(properties.get("propertyName").as(String.class), "hardware.baseboard.manufacturer");
			Predicate valuePredicate = cb.like(properties.get("propertyValue").as(String.class), brand.get());
			predicates.add(cb.and(namePredicate, valuePredicate));

			//for count 
			Join<AgentImpl, AgentPropertyImpl> propertiesCount = fromCount.join("properties");
			Predicate namePredicateCount = cbCount.like(propertiesCount.get("propertyName").as(String.class), "hardware.baseboard.manufacturer");
			Predicate valuePredicateCount = cbCount.like(propertiesCount.get("propertyValue").as(String.class), brand.get());
			predicatesCount.add(cbCount.and(namePredicateCount, valuePredicateCount));
		}

		if(model.isPresent() && !model.get().equals("")) {
			Join<AgentImpl, AgentPropertyImpl> properties = from.join("properties");
			Predicate namePredicate = cb.like(properties.get("propertyName").as(String.class), "hardware.baseboard.productName");
			Predicate valuePredicate = cb.like(properties.get("propertyValue").as(String.class), model.get());
			predicates.add(cb.and(namePredicate, valuePredicate));

			//for count 
			Join<AgentImpl, AgentPropertyImpl> propertiesCount = fromCount.join("properties");
			Predicate namePredicateCount = cbCount.like(propertiesCount.get("propertyName").as(String.class), "hardware.baseboard.productName");
			Predicate valuePredicateCount = cbCount.like(propertiesCount.get("propertyValue").as(String.class), model.get());
			predicatesCount.add(cbCount.and(namePredicateCount, valuePredicateCount));
		}

		if(processor.isPresent() && !processor.get().equals("")) {
			Join<AgentImpl, AgentPropertyImpl> properties = from.join("properties");
			Predicate namePredicate = cb.like(properties.get("propertyName").as(String.class), "processor");
			Predicate valuePredicate = cb.like(properties.get("propertyValue").as(String.class), processor.get());
			predicates.add(cb.and(namePredicate, valuePredicate));

			//for count 
			Join<AgentImpl, AgentPropertyImpl> propertiesCount = fromCount.join("properties");
			Predicate namePredicateCount = cbCount.like(propertiesCount.get("propertyName").as(String.class), "processor");
			Predicate valuePredicateCount = cbCount.like(propertiesCount.get("propertyValue").as(String.class), processor.get());
			predicatesCount.add(cbCount.and(namePredicateCount, valuePredicateCount));
		}
		
		if(osVersion.isPresent() && !osVersion.get().equals("" ) && !osVersion.get().equals("null")) {
			Join<AgentImpl, AgentPropertyImpl> properties = from.join("properties");
			Predicate namePredicate = cb.like(properties.get("propertyName").as(String.class), "os.version");
			Predicate valuePredicate = cb.like(properties.get("propertyValue").as(String.class), osVersion.get());
			predicates.add(cb.and(namePredicate, valuePredicate));

			//for count 
			Join<AgentImpl, AgentPropertyImpl> propertiesCount = fromCount.join("properties");
			Predicate namePredicateCount = cbCount.like(propertiesCount.get("propertyName").as(String.class), "os.version");
			Predicate valuePredicateCount = cbCount.like(propertiesCount.get("propertyValue").as(String.class), osVersion.get());
			predicatesCount.add(cbCount.and(namePredicateCount, valuePredicateCount));
		}
		
		if(agentVersion.isPresent() && !agentVersion.get().equals("")) {
			Join<AgentImpl, AgentPropertyImpl> properties = from.join("properties");
			Predicate namePredicate = cb.like(properties.get("propertyName").as(String.class), "agentVersion");
			Predicate valuePredicate = cb.like(properties.get("propertyValue").as(String.class), agentVersion.get());
			predicates.add(cb.and(namePredicate, valuePredicate));

			//for count 
			Join<AgentImpl, AgentPropertyImpl> propertiesCount = fromCount.join("properties");
			Predicate namePredicateCount = cbCount.like(propertiesCount.get("propertyName").as(String.class), "agentVersion");
			Predicate valuePredicateCount = cbCount.like(propertiesCount.get("propertyValue").as(String.class), agentVersion.get());
			predicatesCount.add(cbCount.and(namePredicateCount, valuePredicateCount));
		}
		
		if(diskType.isPresent() && !diskType.get().equals("") && !diskType.get().equals("ALL")) {
			
			Join<AgentImpl, AgentPropertyImpl> properties = from.join("properties");
			Predicate namePredicate = cb.like(properties.get("propertyName").as(String.class),diskType.get());
			predicates.add(namePredicate);

			//for count 
			Join<AgentImpl, AgentPropertyImpl> propertiesCount = fromCount.join("properties");
			Predicate namePredicateCount = cbCount.like(propertiesCount.get("propertyName").as(String.class),diskType.get());
			predicatesCount.add(namePredicateCount);
		}
		
		if(sessionReportType.isPresent() && !sessionReportType.get().equals("")) {
//		if(sessionReportType != null) {
			Date sessionFilterDate = null;
			Date now = new Date();
			if(sessionReportType.get().equals("LAST_ONE_MONTH_NO_SESSIONS")) { 
				predicates.add(
						cb.not(from.get("jid").in(
								!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList("")))));
				predicatesCount.add(
						cb.not(fromCount.get("jid").in(
								!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList("")))));
				
				sessionFilterDate = Date.from(ZonedDateTime.now().minusMonths(1).toInstant());
				predicates.add(cb.between(from.get("lastLoginDate"), sessionFilterDate, new Date()).not());
				predicatesCount.add(cbCount.between(fromCount.get("lastLoginDate"), sessionFilterDate, now).not());
			} else if(sessionReportType.get().equals("LAST_TWO_MONTHS_NO_SESSIONS")) {
				predicates.add(
						cb.not(from.get("jid").in(
								!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList("")))));
				predicatesCount.add(
						cbCount.not(fromCount.get("jid").in(
								!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList("")))));
				
				sessionFilterDate = Date.from(ZonedDateTime.now().minusMonths(2).toInstant());
				predicates.add(cb.between(from.get("lastLoginDate"), sessionFilterDate, new Date()).not());
				predicatesCount.add(cbCount.between(fromCount.get("lastLoginDate"), sessionFilterDate, now).not());
			} else if(sessionReportType.get().equals("LAST_THREE_MONTHS_NO_SESSIONS")) {
				predicates.add(
						cb.not(from.get("jid").in(
								!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList("")))));
				predicatesCount.add(
						cbCount.not(fromCount.get("jid").in(
								!CollectionUtils.isEmpty(listOfOnlineUsers)? listOfOnlineUsers : new ArrayList<String>(Arrays.asList("")))));
				
				sessionFilterDate = Date.from(ZonedDateTime.now().minusMonths(3).toInstant());
				predicates.add(cb.between(from.get("lastLoginDate"), sessionFilterDate, new Date()).not());
				predicatesCount.add(cbCount.between(fromCount.get("lastLoginDate"), sessionFilterDate, now).not());
			} else if(sessionReportType.get().equals("LAST_ONE_MONTH_SESSIONS")) {
				sessionFilterDate = Date.from(ZonedDateTime.now().minusMonths(1).toInstant());
				predicates.add(cb.between(from.get("lastLoginDate"), sessionFilterDate, new Date()));
				predicatesCount.add(cbCount.between(fromCount.get("lastLoginDate"), sessionFilterDate, now));
			} else if(sessionReportType.get().equals("LAST_TWO_MONTHS_SESSIONS")) {
				sessionFilterDate = Date.from(ZonedDateTime.now().minusMonths(2).toInstant());
				predicates.add(cb.between(from.get("lastLoginDate"), sessionFilterDate, new Date()));
				predicatesCount.add(cbCount.between(fromCount.get("lastLoginDate"), sessionFilterDate, now));
			} else if(sessionReportType.get().equals("LAST_THREE_MONTHS_SESSIONS")) {
				sessionFilterDate = Date.from(ZonedDateTime.now().minusMonths(3).toInstant());
				predicates.add(cb.between(from.get("lastLoginDate"), sessionFilterDate, new Date()));
				predicatesCount.add(cbCount.between(fromCount.get("lastLoginDate"), sessionFilterDate, now));
			}

		}

		criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));
		criteriaQuery.orderBy(cb.desc(from.get("createDate")));
		Long count = count(cbCount, predicatesCount, criteriaCount);

		TypedQuery<AgentImpl> typedQuery = entityManager.createQuery(select);
		typedQuery.setFirstResult((pageNumber - 1)*pageSize);
		if(pageNumber*pageSize > count) {
			typedQuery.setMaxResults((int) (count%pageSize));
		} else {
			typedQuery.setMaxResults(pageSize);
		}

		Page<AgentImpl> agents = new PageImpl<AgentImpl>(typedQuery.getResultList(), pageable, count);

		return agents;
	}

	/*
	 * get count of filtered data for paging
	 */
	public Long count(CriteriaBuilder builder, List<Predicate> restrictions, CriteriaQuery<Long> criteria ) {
		criteria.where(restrictions.toArray(new Predicate[restrictions.size()]));
		TypedQuery<Long> query = entityManager.createQuery(criteria);
		return query.getSingleResult();
	}
}