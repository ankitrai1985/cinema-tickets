
package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private static final int ADULT_PRICE = 25;
    private static final int CHILD_PRICE = 15;
    private static final int MAX_TICKETS = 25;

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl() {
        this(new TicketPaymentServiceImpl(), new SeatReservationServiceImpl());
    }

    public TicketServiceImpl(TicketPaymentService paymentService,
                             SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
        throws InvalidPurchaseException {

    // Check the account and ticket requests before processing.
    if (accountId == null || accountId <= 0) {
        throw new InvalidPurchaseException("Invalid account ID");
    }

    if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
        throw new InvalidPurchaseException("No tickets requested");
    }

    int adults = 0;
    int children = 0;
    int infants = 0;

    // Count each ticket type. The same type may appear more than once.
    for (TicketTypeRequest request : ticketTypeRequests) {

        if (request == null || request.getTicketType() == null) {
            throw new InvalidPurchaseException("Invalid ticket request");
        }

        int quantity = request.getNoOfTickets();

        if (quantity <= 0) {
            throw new InvalidPurchaseException("Ticket quantity must be positive");
        }

        switch (request.getTicketType()) {
            case ADULT -> adults += quantity;
            case CHILD -> children += quantity;
            case INFANT -> infants += quantity;
        }
    }

    int totalTickets = adults + children + infants;

    // The 25-ticket limit includes infants, even though they are free.
    if (totalTickets > MAX_TICKETS) {
        throw new InvalidPurchaseException("Maximum 25 tickets allowed");
    }

    // Children and infants must be accompanied by an adult.
    if (adults == 0) {
        throw new InvalidPurchaseException("At least one adult ticket is required");
    }

    // Infants are free and sit on an adult's lap, so they don't need seats.
    int totalAmount = (adults * ADULT_PRICE) + (children * CHILD_PRICE);
    int totalSeats = adults + children;

    // Only call the external services once all checks have passed.
    paymentService.makePayment(accountId, totalAmount);
    reservationService.reserveSeat(accountId, totalSeats);
}
}

