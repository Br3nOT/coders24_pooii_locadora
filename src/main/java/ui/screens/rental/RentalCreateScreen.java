package ui.screens.rental;

import dto.CreateRentalDTO;
import enums.CustomerType;
import model.agency.Agency;
import model.customer.Customer;
import model.rental.Rental;
import model.vehicle.Vehicle;
import service.agency.AgencyService;
import service.customer.CustomerService;
import service.rental.RentalService;
import service.vehicle.VehicleService;
import ui.utils.Header;
import ui.core.Screen;
import ui.flow.FlowController;
import ui.screens.agency.AgencyListScreen;
import ui.screens.customer.CustomerListScreen;
import ui.screens.vehicle.VehicleListScreen;
import ui.utils.Input;
import ui.utils.Output;
import ui.utils.Result;
import ui.utils.ScreenUtils;
import utils.DateTimeUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class RentalCreateScreen extends Screen {
    private static final int MAX_LINE_LENGTH = 65;
    private final Scanner scanner;

    private final AgencyService agencyService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final RentalService rentalService;

    private Agency selectedAgency;
    private Customer selectedCustomer;
    private Vehicle selectedVehicle;

    private LocalDateTime startDate;
    private LocalDateTime estimatedEndDate;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    private int rentalDays;

    private int currentField = 0;

    private boolean isAgencySelectionListCalled = false;
    private boolean isCustomerSelectionListCalled = false;
    private boolean isVehicleSelectionListCalled = false;

    public RentalCreateScreen(FlowController flowController,
                              Scanner scanner, AgencyService agencyService,
                              VehicleService vehicleService,
                              CustomerService customerService, RentalService rentalService) {
        super(flowController);
        this.scanner = scanner;
        this.agencyService = agencyService;
        this.vehicleService = vehicleService;
        this.customerService = customerService;
        this.rentalService = rentalService;
    }

    @Override
    public void show() {
        do {
            ScreenUtils.clearScreen();

            Header.show("Preencha os campos abaixo para cadastrar a locação.", null);

            displayRentalRegistration();

            Output.info("'V' para voltar campo, 'C' para cancelar o cadastro.");

            switch (currentField) {
                case 0 -> {
                    if (!isAgencySelectionListCalled) {
                        isAgencySelectionListCalled = true;

                        AgencyListScreen agencyListScreen = new AgencyListScreen(flowController, scanner, agencyService, true);
                        flowController.goTo(agencyListScreen);

                        selectedAgency = agencyListScreen.getSelectedAgency();

                        flowController.goBack();
                    }

                    if (selectedAgency == null) {
                        Output.error("Você precisa selecionar uma agência válida!");

                        handleFieldSelectionError();
                        break;
                    }
                    currentField = 1;
                }
                case 1 -> {
                    if (!isVehicleSelectionListCalled) {
                        isVehicleSelectionListCalled = true;

                        VehicleListScreen vehicleListScreen = new VehicleListScreen(flowController, scanner, vehicleService, true, selectedAgency.getId());
                        flowController.goTo(vehicleListScreen);

                        selectedVehicle = vehicleListScreen.getSelectedVehicle();

                        flowController.goBack();
                    }

                    if (selectedVehicle == null) {
                        Output.error("Você precisa selecionar uma veículo válido!");


                        handleFieldSelectionError();
                        break;
                    }
                    currentField = 2;
                }
                case 2 -> {
                    if (!isCustomerSelectionListCalled) {
                        isCustomerSelectionListCalled = true;

                        CustomerListScreen customerListScreen = new CustomerListScreen(flowController, scanner, customerService, true);
                        flowController.goTo(customerListScreen);

                        selectedCustomer = customerListScreen.getSelectedCustomer();

                        flowController.goBack();
                    }

                    if (selectedVehicle == null) {
                        Output.error("Você precisa selecionar um cliente válido!");

                        handleFieldSelectionError();
                        break;
                    }
                    currentField = 3;
                }
                case 3 -> {
                    startDate = LocalDateTime.now();

                    estimatedEndDate = null;

                    while (estimatedEndDate == null) {
                        Result<LocalDateTime> estimatedReturnResult = Input.getAsDateTime(scanner, "Data de retorno (dd/MM/yyyy HH:mm): ", false);
                        if (estimatedReturnResult.isFailure()) {
                            // TODO: errorMessage
                            Output.error(estimatedReturnResult.getErrorMessage());
                            continue;
                        }
                        estimatedEndDate = estimatedReturnResult.getValue();
                    }

                    rentalDays = (int) DateTimeUtils.calculateDaysBetween(startDate, estimatedEndDate);

                    Output.info(String.format("Data de início: %s", startDate.format(formatter)));
                    Output.info(String.format("Data estimada de término: %s", estimatedEndDate.format(formatter)));
                    Output.info(String.format("Valor estimado: R$ %.2f", selectedVehicle.getDailyRate().multiply(BigDecimal.valueOf(rentalDays))));

                    currentField = 5;
                }

                case 5 -> confirmRegistration();
            }
        } while (true);
    }

    private void displayRentalRegistration() {
        String[] fields = {
                "Agência: " + (selectedAgency != null ? selectedAgency.getName() : ""),
                "Veículo: " + (selectedVehicle != null ? selectedVehicle.getModel() : ""),
                "Cliente: " + (selectedCustomer != null ? selectedCustomer.getName() : ""),
                "Data de início: " + (startDate != null ? startDate.format(formatter) : ""),
                "Data estimada de término: " + (estimatedEndDate != null ? estimatedEndDate.format(formatter) : ""),
                "Custo estimado da locação: " + (selectedVehicle != null && estimatedEndDate != null
                        ? "R$ " + (selectedVehicle.getDailyRate().multiply(BigDecimal.valueOf(rentalDays)))
                        : "")
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

    private void confirmRegistration() {
        String input = Input.getAsString(scanner, "Confirma o cadastro? (S/n): ", true, false);
        input = input.isEmpty() ? "s" : input;

        if (input.equalsIgnoreCase("s")) {
            // Chamar o serviço de cadastro

            CreateRentalDTO createRentalDTO = new CreateRentalDTO(
                    selectedCustomer, selectedVehicle, selectedAgency, startDate, estimatedEndDate
            );

            try {
                Rental createdRental = rentalService.createRental(createRentalDTO);

                showReceipt(createdRental);

            } catch (Exception e) {
                Output.error(e.getMessage());
                System.out.println("... cancelado.");
                scanner.nextLine();
                cancelRegistration();
                return;
            }


        } else {
            System.out.println("Cancelada.");
        }
        scanner.nextLine();
        flowController.goBack();
    }

    private void showReceipt(Rental rental) {
        System.out.println(rental.generatePickupReceipt());
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
                if (currentField == 1) isVehicleSelectionListCalled = false;
                if (currentField == 2) isCustomerSelectionListCalled = false;
            }
            case 3 -> cancelRegistration();
        }
    }

}
