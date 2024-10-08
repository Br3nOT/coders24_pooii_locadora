package ui.screens.vehicle;

import model.vehicle.Vehicle;
import service.vehicle.VehicleService;
import ui.utils.Header;
import ui.core.Screen;
import ui.flow.FlowController;
import ui.utils.Input;
import ui.utils.Output;
import ui.utils.ScreenUtils;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class VehicleListScreen extends Screen {
    private static final int MAX_LINE_LENGTH = 65;
    private final Scanner scanner;
    private final VehicleService vehicleService;
    private final boolean isModal;
    private final String agencyIdToFilter;

    private Vehicle selectedVehicle;
    private String searchQuery = "";

    private int currentPage = 0;
    private static final int PAGE_SIZE = 2;
    private List<Vehicle> filteredVehicles;

    public VehicleListScreen(FlowController flowController, Scanner scanner, VehicleService vehicleService, boolean isModal, String agencyIdToFilter) {
        super(flowController);
        this.scanner = scanner;
        this.vehicleService = vehicleService;
        this.isModal = isModal;
        this.agencyIdToFilter = agencyIdToFilter;
    }

    private boolean vehicleSelected = false;

    @Override
    public void show() {
        List<Vehicle> vehicles;
        if (agencyIdToFilter != null) {
            vehicles = vehicleService.findAvailableVehiclesByAgencyId(agencyIdToFilter);
        } else {
            vehicles = vehicleService.findAllVehicles();
        }
        this.filteredVehicles = vehicles;

        do {
            ScreenUtils.clearScreen();

            listPaginatedAgencies(filteredVehicles, currentPage);

            Output.info("'X' para voltar");

            String promptMessage = isModal ? "Selecione um veículo pelo número ou utilize os comandos acima: "
                    : "Utilize os comandos de navegação: ";

            String input = Input.getAsString(scanner, promptMessage, true, false);

            if (processInputCommands(input, vehicles)) {
                break;
            }

        } while (!vehicleSelected);
    }

    private void listPaginatedAgencies(List<Vehicle> vehicles, int page) {
        int totalPages = (int) Math.ceil((double) vehicles.size() / PAGE_SIZE);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, vehicles.size());

        if (isModal) {
            Header.show("Selecione um veículo para continuar...", null);
        } else {
            Header.show("Lista de Veículos", null);
        }


        if (!searchQuery.isEmpty()) {
            System.out.println("║  Filtro: " + searchQuery);
        }

        if (vehicles.isEmpty()) {
            Output.info("Nenhuma veículo encontrado.\n");
            return;
        }

        String emptyLine = "║    " + " ".repeat(MAX_LINE_LENGTH) + "    ║";
        String bottomLine = "╚════" + "═".repeat(MAX_LINE_LENGTH) + "════╝";

        System.out.println(emptyLine);
        System.out.printf("║ %-3s │ %-10s │ %-10s │ %-12s │ %-12s │ %-9s ║%n", "Nº", "Tipo", "Placa", "Modelo", "Marca", "Diária");
        System.out.println("╟─────┼────────────┼────────────┼──────────────┼──────────────┼───────────╢");

        for (int i = start; i < end; i++) {
            Vehicle vehicle = vehicles.get(i);
            System.out.printf("║ %-3d │ %-10s │ %-10s │ %-12s │ %-12s │ %9s ║%n",
                    (i + 1),
                    limitString(vehicle.getType().getDescription(), 10),
                    limitString(vehicle.getPlate(), 10),
                    limitString(vehicle.getModel(), 12),
                    limitString(vehicle.getBrand(), 12),
                    limitString(vehicle.getDailyRate().toString(), 9));
        }

        System.out.println(emptyLine);
        System.out.println(bottomLine);

        System.out.println("\nPágina " + (page + 1) + " de " + totalPages + "\n");

        Output.info("'F' para filtrar, 'L' para limpar filtro.");
        Output.info("'A' para avançar página, 'V' para voltar página");

    }

    private String limitString(String str, int maxLength) {
        if (str.length() > maxLength) {
            return str.substring(0, maxLength - 3) + "...";
        }
        return str;
    }

    private boolean processInputCommands(String input, List<Vehicle> vehicles) {
        switch (input.toLowerCase()) {
            case "v":
                if (currentPage > 0) {
                    currentPage--;
                }
                break;

            case "a":
                if (currentPage < (int) Math.ceil((double) filteredVehicles.size() / PAGE_SIZE) - 1) {
                    currentPage++;
                }
                break;

            case "f":
                searchVehicles(vehicles);
                break;

            case "l":
                filteredVehicles = vehicles;
                currentPage = 0;
                break;

            case "x":
                back();
                return true;

            default:
                try {
                    int vehicleIndex = Integer.parseInt(input) - 1;
                    if (vehicleIndex >= 0 && vehicleIndex < filteredVehicles.size()) {
                        selectVehicle(filteredVehicles.get(vehicleIndex));
                        vehicleSelected = true;
                        return true;
                    } else {
                        System.out.println("Opção inválida.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Entrada inválida. Tente novamente.");
                }
                break;
        }
        return false;
    }

    private void searchVehicles(List<Vehicle> vehicles) {
        System.out.println("Digite o modelo que deseja buscar: ");
        searchQuery = scanner.nextLine().trim().toLowerCase();

        filteredVehicles = vehicles.stream()
                .filter(vehicle -> vehicle.getModel().toLowerCase().contains(searchQuery))
                .collect(Collectors.toList());

        currentPage = 0;

        if (filteredVehicles.isEmpty()) {
            System.out.println("Nenhum veículo encontrado com o modelo: " + searchQuery);
            filteredVehicles = vehicles;
        } else {
            System.out.println(filteredVehicles.size() + " veículo(s) encontrado(s).");
        }

        System.out.println("Pressione Enter para continuar.");
        scanner.nextLine();
    }

    private void selectVehicle(Vehicle vehicle) {
        this.selectedVehicle = vehicle;
        this.vehicleSelected = true;
    }

    private void back() {
        flowController.goBack();
    }

    public Vehicle getSelectedVehicle() {
        return selectedVehicle;
    }
}
