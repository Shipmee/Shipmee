package repositories;

import java.util.Date;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import domain.Shipment;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {
	
	@Query("select s from Shipment s where s.origin LIKE %?1% AND s.destination LIKE %?2% AND (?3 IS NULL OR s.date = ?3)"
			+ "AND (?4 IS NULL OR r.departureTime = ?4) AND (?5 IS NULL OR r.itemEnvelope = ?5) AND (?6 IS NULL OR r.itemSize = ?6)")
	Collection<Shipment> searchShipment(String origin, String destination, Date date, Date time, String envelope, String itemSize);

}
