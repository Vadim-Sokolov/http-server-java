public class Main {

    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        if (args.length > 1 && args[0].equals("--directory")) {
            AppConfig.directory = args[1];
        }

        HTSController controller = new HTSController();
        controller.run();
    }
}
