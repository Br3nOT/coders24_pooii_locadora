package ui.screens.rental;

import model.agency.Agency;
import model.rental.Rental;
import service.agency.AgencyService;
import service.rental.RentalService;
import ui.utils.Header;
import ui.core.Screen;
import ui.flow.FlowController;
import ui.screens.agency.AgencyListScreen;
import ui.utils.Input;
import ui.utils.Output;
import ui.utils.Result;
import ui.utils.ScreenUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class RentalCloseScreen extends Screen {
    private static final int MAX_LINE_LENGTH = 65;
    private final Scanner scanner;

    private final AgencyService agencyService;
    private final RentalService rentalService;

    private Rental rentalToClose;
    private Agency returnAgency;

    private LocalDateTime returnDate;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    private String errorMessage = "";

    private int currentField = 0;

    private boolean isRentalSelectionListCalled = false;
    private boolean isAgencySelectionListCalled = false;

    public RentalCloseScreen(FlowController flowController,
                             Scanner scanner, AgencyService agencyService,
                             RentalService rentalService) {
        super(flowController);
        this.scanner = scanner;
        this.agencyService = agencyService;
        this.rentalService = rentalService;
    }

    @Override
    public void show() {
        do {
            ScreenUtils.clearScreen();

            Header.show("Preencha os campos abaixo para encerrar a locação.", null);

            if (rentalToClose != null) {

                displayRentalClose();

                Output.info("'V' para voltar campo, 'C' para cancelar o cadastro.");

            }

            displayPendingMessages();

            switch (currentField) {
                case 0 -> {
                    if (!isRentalSelectionListCalled) {
                        isRentalSelectionListCalled = true;

                        OpenRentalListScreen openRentalListScreen = new OpenRentalListScreen(flowController, scanner, rentalService, true);
                        flowController.goTo(openRentalListScreen);

                        rentalToClose = openRentalListScreen.getSelectedRental();

                        flowController.goBack();
                    }

                    if (rentalToClose == null) {
                        Output.error("Você precisa selecionar uma locação válida!");

                        handleFieldSelectionError();
                        break;
                    }
                    currentField = 1;
                }
                case 1 -> {
                    if (!isAgencySelectionListCalled) {
                        isAgencySelectionListCalled = true;

                        AgencyListScreen agencyListScreen = new AgencyListScreen(flowController, scanner, agencyService, true);
                        flowController.goTo(agencyListScreen);

                        returnAgency = agencyListScreen.getSelectedAgency();

                        flowController.goBack();
                    }

                    if (returnAgency == null) {
                        Output.error("Você precisa selecionar uma agência válida!");

                        handleFieldSelectionError();
                        break;
                    }
                    currentField = 2;
                }
                case 2 -> {
                    returnDate = null;

                    while (returnDate == null) {
                        Result<LocalDateTime> returnDateInput = Input.getAsDateTime(scanner, "Data de retorno (dd/MM/yyyy HH:mm): ", false);
                        if (returnDateInput.isFailure()) {
                            // TODO: errorMessage
                            Output.error(returnDateInput.getErrorMessage());
                            continue;
                        }
                        returnDate = returnDateInput.getValue();
                    }

                    currentField = 3;
                }

                case 3 -> confirmClose();
            }
        } while (true);
    }

    private void displayPendingMessages() {
        if (!errorMessage.isEmpty()) {
            Output.error(errorMessage);
            // Esperar o usuário pressionar Enter para continuar
            Input.getAsString(scanner, "Pressione Enter para continuar...", true, false);
            errorMessage = ""; // Limpa o erro após o usuário confirmar
        }
    }

    private void displayRentalClose() {

        String[] fields = {
                "Agência de Retirada: " + rentalToClose.getPickUpAgency().getName(),
                "Veículo: " + rentalToClose.getVehicle().getModel(),
                "Cliente: " + rentalToClose.getCustomer().getName(),
                "Data de início: " + rentalToClose.getPickUpDate().format(formatter),
                "Data estimada de término: " + rentalToClose.getEstimatedReturnDate().format(formatter),
                "Agência de Retorno: " + (returnAgency != null ? returnAgency.getName() : ""),
                "Data de Retorno: " + (returnDate != null ? returnDate.format(formatter) : ""),
        };

        String emptyLine = "║    " + " ".repeat(MAX_LINE_LENGTH) + "    ║";
        String bottomLine = "╚════" + "═".repeat(MAX_LINE_LENGTH) + "════╝";

        System.out.println(emptyLine);

        for (String field : fields) {
            System.out.printf("║    %-65s    ║%n", field);
        }
        System.out.println(emptyLine);
        System.out.println(bottomLine);
    }

    private void confirmClose() {
        String input = Input.getAsString(scanner, "Confirma o enceramento? (S/n): ", true, false);
        input = input.isEmpty() ? "s" : input;

        if (input.equalsIgnoreCase("s")) {
            // Chamar o serviço de encerramento

            try {
                closeRental();
            } catch (Exception e) {
                handleError(e.getMessage());
                return;
            }

            showReceipt(rentalToClose);
        } else {
            System.out.println("Cancelada.");
        }
        scanner.nextLine();
        flowController.goBack();
    }

    private void closeRental() {
        rentalService.closeRental(rentalToClose, returnAgency, returnDate);
    }

    private void showReceipt(Rental rental) {
        System.out.println(rental.generateReturnReceipt());
    }

    private void handleError(String message) {
        errorMessage = message;
        currentField = 0;
    }

    private boolean processInputCommands(String input) {
        if (input.equalsIgnoreCase("v")) {
            if (currentField > 0) {
                currentField--;
            }
            return true;
        } else if (input.equalsIgnoreCase("c")) {
            cancelRegistration();
            return true;
        }
        return false;
    }

    private void cancelRegistration() {
        flowController.goBack();
    }

    private void handleFieldSelectionError() {
        String[] options = {
                "  [ 1 ] - Voltar para o campo anterior",
                "  [ 2 ] - Tentar novamente",
                "  [ 3 ] - Cancelar"
        };

        String emptyLine = "║  " + " ".repeat(40) + "  ║";
        String bottomLine = "╚══" + "═".repeat(40) + "══╝";

        System.out.println(emptyLine);
        for (String option : options) {
            System.out.printf("║  %-40s  ║%n", option);
        }
        System.out.println(emptyLine);
        System.out.println(bottomLine);

        Result<Integer> option = Input.getAsInt(scanner, "Escolha uma opção: ", false);
        switch (option.getValue()) {
            case 1 -> {
                if (currentField > 0) {
                    currentField--;
                }
            }
            case 2 -> {
                if (currentField == 0) isAgencySelectionListCalled = false;
                if (currentField == 1) isAgencySelectionListCalled = false;
            }
            case 3 -> cancelRegistration();
        }
    }

}
