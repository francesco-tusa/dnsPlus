package simulator;

import java.util.Scanner;

/**
 * Main entry point for the entire Pub/Sub Simulator.
 * Provides a menu to select which simulation suite to run.
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n======================================");
            System.out.println("  Pub/Sub Simulator - Main Menu");
            System.out.println("======================================");
            System.out.println("1. Functional Tests (Region-Based)");
            System.out.println("2. Functional Tests (Location-Based)");
            System.out.println("3. Performance Tests (Region-Based)");
            System.out.println("4. Performance Tests (Location-Based)");
            System.out.println("5. Exit");
            System.out.print("\nPlease choose an option (1-5): ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    RegionFunctionalTestsMain.main(args);
                    break;
                case "2":
                    LocationFunctionalTestsMain.main(args);
                    break;
                case "3":
                    RegionPerformanceSimulationsMain.main(args);
                    break;
                case "4":
                    LocationPerformanceSimulationsMain.main(args);
                    break;
                case "5":
                    System.out.println("Exiting simulator.");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        }
    }
}