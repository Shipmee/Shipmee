package services.form;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import domain.Route;
import domain.SizePrice;
import domain.form.SizePriceForm;
import services.RouteService;
import services.SizePriceService;

@Service
@Transactional
public class SizePriceFormService {

	// Supporting services ----------------------------------------------------

	@Autowired
	private SizePriceService sizePriceService;
	
	@Autowired
	private RouteService routeService;
	
	// Constructors -----------------------------------------------------------

	public SizePriceFormService() {
		super();
	}

	// Simple CRUD methods ----------------------------------------------------

	public SizePriceForm create(int routeId) {
		SizePriceForm result;
		
		result = new SizePriceForm();
		
		result.setRouteId(routeId);
		
		return result;
	}
	
	public Collection<SizePrice> reconstruct(SizePriceForm sizePriceForm) {
		Collection<SizePrice> result;
						
		this.delete(sizePriceForm);
		result = saveCreate(sizePriceForm);
		
		return result;
	}

	public SizePriceForm findOne(int routeId) {
		SizePriceForm result;
		
		result = this.create(routeId);
		
		result = constructForm(result);
		
		return result;
	}

	public void delete(SizePriceForm sizePriceForm) {
		Collection<SizePrice> res;
		
		res = sizePriceService.findAllByRouteId(sizePriceForm.getRouteId());
		
		for(SizePrice sp : res) {
			sizePriceService.delete(sp);
		}
	}
	
	private Collection<SizePrice> saveCreate(SizePriceForm sizePriceForm) {
		Collection<SizePrice> result;
		SizePrice sizePrice;
		Route route;
		
		result = new ArrayList<SizePrice>();
		route = routeService.findOne(sizePriceForm.getRouteId());
		
		if(sizePriceForm.isS()) {
			sizePrice = sizePriceService.create();
			
			sizePrice.setSize("S");
			sizePrice.setPrice(sizePriceForm.getPriceS());
			sizePrice.setRoute(route);
			
			sizePrice = sizePriceService.save(sizePrice);
			result.add(sizePrice);
		}
		
		if(sizePriceForm.isM()) {
			sizePrice = sizePriceService.create();
			
			sizePrice.setSize("M");
			sizePrice.setPrice(sizePriceForm.getPriceM());
			sizePrice.setRoute(route);
			
			sizePrice = sizePriceService.save(sizePrice);
			result.add(sizePrice);
		}
		
		if(sizePriceForm.isL()) {
			sizePrice = sizePriceService.create();
			
			sizePrice.setSize("L");
			sizePrice.setPrice(sizePriceForm.getPriceL());
			sizePrice.setRoute(route);
			
			sizePrice = sizePriceService.save(sizePrice);
			result.add(sizePrice);
		}
		
		if(sizePriceForm.isXL()) {
			sizePrice = sizePriceService.create();
			
			sizePrice.setSize("XL");
			sizePrice.setPrice(sizePriceForm.getPriceXL());
			sizePrice.setRoute(route);
			
			sizePrice = sizePriceService.save(sizePrice);
			result.add(sizePrice);
		}
		
		return result;
	}
	
	private SizePriceForm constructForm(SizePriceForm result) {
		Collection<SizePrice> sizePrices;
		
		sizePrices = sizePriceService.findAllByRouteId(result.getRouteId());
		
		for(SizePrice sp : sizePrices) {
			if(sp.getSize().equals("S")) {
				result.setS(true);
				result.setPriceS(sp.getPrice());
			} else if(sp.getSize().equals("M")) {
				result.setM(true);
				result.setPriceM(sp.getPrice());
			} else if(sp.getSize().equals("L")) {
				result.setL(true);
				result.setPriceL(sp.getPrice());
			} else if(sp.getSize().equals("XL")) {
				result.setXL(true);
				result.setPriceXL(sp.getPrice());
			}
		}
		
		return result;
	}
}