
package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

class TicketServiceImplTest {

    private TicketPaymentService paymentService;
    private SeatReservationService reservationService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        reservationService = mock(SeatReservationService.class);

        ticketService = new TicketServiceImpl(
                paymentService, reservationService);
    }

    @Test
    void shouldPurchaseAdultTickets() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(ADULT, 2));

        verify(paymentService).makePayment(1L, 50);
        verify(reservationService).reserveSeat(1L, 2);
    }

    @Test
    void shouldCalculatePaymentAndSeatsCorrectly() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(ADULT, 2),
                new TicketTypeRequest(CHILD, 3),
                new TicketTypeRequest(INFANT, 1));

        verify(paymentService).makePayment(1L, 95);
        verify(reservationService).reserveSeat(1L, 5);
    }

    @Test
    void shouldNotChargeOrReserveSeatForInfants() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(INFANT, 2));

        verify(paymentService).makePayment(1L, 25);
        verify(reservationService).reserveSeat(1L, 1);
    }

    @Test
    void shouldAllowMaximum25Tickets() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(CHILD, 24));

        verify(paymentService).makePayment(1L, 385);
        verify(reservationService).reserveSeat(1L, 25);
    }

    @Test
    void shouldRejectMoreThan25Tickets() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(ADULT, 2),
                        new TicketTypeRequest(CHILD, 24)));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectInvalidAccountId() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(0L,
                        new TicketTypeRequest(ADULT, 1)));

        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(-1L,
                        new TicketTypeRequest(ADULT, 1)));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectNullAccountId() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(null,
                        new TicketTypeRequest(ADULT, 1)));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectChildTicketsWithoutAdult() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(CHILD, 2)));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectInfantTicketsWithoutAdult() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(INFANT, 1)));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectZeroOrNegativeQuantity() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(ADULT, 0)));

        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(ADULT, -2)));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectEmptyRequests() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectNullRequests() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(
                        1L, (TicketTypeRequest[]) null));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectNullTicketEntry() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(ADULT, 1),
                        null));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectNullTicketType() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(null, 1)));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldCombineDuplicateTicketTypes() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(ADULT, 2),
                new TicketTypeRequest(CHILD, 1));

        verify(paymentService).makePayment(1L, 90);
        verify(reservationService).reserveSeat(1L, 4);
    }

    @Test
    void shouldRejectRequestIfAnyQuantityIsInvalid() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(ADULT, 1),
                        new TicketTypeRequest(CHILD, -1)));

        verifyNoInteractions(paymentService, reservationService);
    }
}
