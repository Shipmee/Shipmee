package controllers.user;


import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import controllers.AbstractController;
import domain.FeePayment;
import domain.RouteOffer;
import domain.ShipmentOffer;
import domain.form.FeePaymentForm;
import services.FeePaymentService;
import services.RouteOfferService;
import services.RouteService;
import services.ShipmentOfferService;
import services.form.FeePaymentFormService;

@Controller
@RequestMapping("/feepayment/user")
public class FeePaymentUserController extends AbstractController {
	
	// Services ---------------------------------------------------------------
	
	@Autowired
	private FeePaymentService feePaymentService;
	
	@Autowired
	private FeePaymentFormService feePaymentFormService;
	
	@Autowired
	private RouteService routeService;
	
	@Autowired
	private RouteOfferService routeOfferService;
	
	@Autowired
	private ShipmentOfferService shipmentOfferService;
	
	// Constructors -----------------------------------------------------------
	
	public FeePaymentUserController() {
		super();
	}

	// Listing ----------------------------------------------------------------
	
	/*@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ModelAndView list() {
		ModelAndView result;
		Collection<Vehicle> vehicles;
		
		vehicles = feePaymentService.findAllNotDeletedByUser();
		
		result = new ModelAndView("vehicle/list");
		result.addObject("vehicles", vehicles);

		return result;
	}*/

	// Creation ---------------------------------------------------------------

	@RequestMapping(value = "/create", method = RequestMethod.GET)
	public ModelAndView create(@RequestParam int type, @RequestParam int id,
			@RequestParam (required=false, defaultValue="0") Integer sizePriceId, @RequestParam (required=false, defaultValue = "0") Double amount,
			@RequestParam (required=false) String description) {
		
		ModelAndView result;
		FeePaymentForm feePaymentForm;

		feePaymentForm = feePaymentFormService.create(type, id, sizePriceId, amount, description);
		result = createEditModelAndView(feePaymentForm);

		return result;
	}

	// Edition ----------------------------------------------------------------

	@RequestMapping(value = "/create", method = RequestMethod.POST, params = "save")
	public ModelAndView save(@Valid FeePaymentForm feePaymentForm, BindingResult binding) {
		ModelAndView result;
		RouteOffer routeOffer;
		FeePayment feePayment;
		String redirect = null;

		if (binding.hasErrors()) {
			result = createEditModelAndView(feePaymentForm);
		} else {
			try {
				
				/**
				 * Type == 1 -> Contract a route
				 * Type == 2 -> Create a routeOffer
				 * Type == 3 -> Accept a shipmentOffer
				 */
				switch (feePaymentForm.getType()) {
				case 1:
					
					routeOffer = routeService.contractRoute(feePaymentForm.getId(), feePaymentForm.getSizePriceId());
					feePaymentForm.setOfferId(routeOffer.getId());
					redirect = "redirect:../../routeOffer/user/list.do?routeId=" + routeOffer.getRoute().getId();
					break;
					
				case 2:
					routeOffer = routeOfferService.create(feePaymentForm.getId());
					routeOffer.setAmount(feePaymentForm.getAmount());
					routeOffer.setDescription(feePaymentForm.getDescription());
					routeOffer = routeOfferService.save(routeOffer);
					
					feePaymentForm.setOfferId(routeOffer.getId());
					redirect = "redirect:../../routeOffer/user/list.do?routeId=" + routeOffer.getRoute().getId();
					break;
					
				case 3:
					ShipmentOffer shipmentOffer;
					shipmentOffer = shipmentOfferService.accept(feePaymentForm.getOfferId());
					
					feePaymentForm.setOfferId(shipmentOffer.getId());
					redirect = "redirect:../../shipmentOffer/user/list.do?shipmentId="+shipmentOffer.getShipment().getId();
					break;

				default:
					break;
				}
				
				feePayment = feePaymentFormService.reconstruct(feePaymentForm);
				feePaymentService.save(feePayment);
				
				result = new ModelAndView(redirect);
			} catch (Throwable oops) {
				System.out.println(oops);
				result = createEditModelAndView(feePaymentForm, "feePayment.commit.error");				
			}
		}

		return result;
	}
	
	// Ancillary methods ------------------------------------------------------
	
	protected ModelAndView createEditModelAndView(FeePaymentForm feePaymentForm) {
		ModelAndView result;

		result = createEditModelAndView(feePaymentForm, null);
		
		return result;
	}	
	
	protected ModelAndView createEditModelAndView(FeePaymentForm feePaymentForm, String message) {
		ModelAndView result;
						
		result = new ModelAndView("feepayment/create");
		result.addObject("feePaymentForm", feePaymentForm);
		result.addObject("message", message);

		return result;
	}

}