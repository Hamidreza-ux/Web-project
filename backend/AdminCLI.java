import java.util.*;

public class AdminCLI implements Runnable {
    private final LoginServer server = LoginServer.getInstance();
    private final Scanner scanner = new Scanner(System.nanoTime() % 2 == 0 ? System.in : System.in);
    private boolean loggedIn = false;

    // نام کاربری و رمز عبور ثابت ادمین برای ورود به CLI
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    @Override
    public void run() {
        System.out.println("\n=== Commandline management (CLI) ===");

        // مرحله احراز هویت ادمین
        while (!loggedIn) {
            System.out.print("username: ");
            String user = scanner.nextLine();
            System.out.print("password: ");
            String pass = scanner.nextLine();

            if (ADMIN_USER.equals(user) && ADMIN_PASS.equals(pass)) {
                loggedIn = true;
                System.out.println("success! Welcome to panel");
            } else {
                System.out.println("wrong! try again!");
            }
        }

        while (loggedIn) {
            printMenu();
            System.out.print("Choose one option: ");
            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1":
                        showAllUsers();
                        break;
                    case "2":
                        addUserManually();
                        break;
                    case "3":
                        deleteUserManually();
                        break;
                    case "4":
                        showAllGroups();
                        break;
                    case "5":
                        createGroupManually();
                        break;
                    case "6":
                        deleteGroupManually();
                        break;
                    case "7":
                        manageGroupMembers();
                        break;
                    case "8":
                        server.printReportedMessages();
                        break;
                    case "9":
                        loggedIn = false;
                        System.out.println("you have logged out of admin panel");
                        break;
                    default:
                        System.out.println("invalid choice! try again!");
                }
            } catch (Exception e) {
                System.out.println("unknown error!" + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n======= centeral management panel =======");
        System.out.println("1. Display a list of all system users.");
        System.out.println("2. add new user (manual)");
        System.out.println("3. delete a user of system");
        System.out.println("4. display a list of groups and their members");
        System.out.println("5. create a new group");
        System.out.println("6. delete a group");
        System.out.println("7. management of group member");
        System.out.println("8. (Reports)");
        System.out.println("9. (Logout)");
        System.out.println("=========================================");
    }

    // نمایش لیست کاربران
    private void showAllUsers() {
        System.out.println("\n--- List of registered users: ---");
        Map<String, User> users = server.getRegisteredMap();
        if (users.isEmpty()) {
            System.out.println("No user haven't registered on system!");
            return;
        }
        for (User u : users.values()) {
            System.out.printf("username: %s | id: %s\n", u.getUsername(), u.getID());
        }
    }

    // اضافه کردن دستی کاربر
    private void addUserManually() {
        System.out.println("\n--- Manual registration of a new user by the admin ---");
        System.out.print("new username: ");
        String user = scanner.nextLine();
        System.out.print("password: ");
        String pass = scanner.nextLine();
        System.out.print("ID: ");
        String ID = scanner.next();

        if (user.isEmpty() || pass.isEmpty()) {
            System.out.println("username or password can not empty!");
            return;
        }

        SignupResult res = server.register(user, ID, pass, pass);
        if (res == SignupResult.SUCCESS) {
            System.out.println("user added to system successfully.");
        } else {
            System.out.println("system error");
        }
    }

    private void deleteUserManually() {
        System.out.print("\n which one of usernames you want to delete it: ");
        String user = scanner.nextLine();
        boolean ok = server.deleteUserAccount(user);
        if (ok)
            System.out.println("user and all chats deleted forever!");
        else
            System.out.println("user not found!");
    }

    private void showAllGroups() {
        System.out.println("\n--- Groups list and their member ---");
        Set<String> printedGroupIds = new HashSet<>();
        int count = 0;

        for (List<ChatRoom> chats : server.getUserChatsMap().values()) {
            for (ChatRoom room : chats) {
                if (room.isGroup() && !printedGroupIds.contains(room.getId())) {
                    printedGroupIds.add(room.getId());
                    count++;
                    System.out.printf("[%d] group name: %s | group id: %s\n", count, room.getName(), room.getId());
                    System.out.println("member: " + room.getMembers());
                }
            }
        }
        if (count == 0)
            System.out.println("no group hasn't resgistered on system!");
    }

    private void createGroupManually() {
        System.out.print("\nnew group name: ");
        String groupName = scanner.nextLine();
        if (groupName.isEmpty())
            return;

        boolean ok = server.createNewGroup("admin", groupName, new ArrayList<>());
        if (ok)
            System.out.println("the group create successfully.");
    }

    private void deleteGroupManually() {
        System.out.print("\n");
        String groupId = scanner.nextLine();
        boolean ok = server.adminDeleteGroup(groupId);
        if (ok)
            System.out.println("group is deleted.");
        else
            System.out.println("no group found");
    }
    //حذف و اضافه اعضا از گروه
    private void manageGroupMembers() {
        System.out.print("\ngroup id: ");
        String groupId = scanner.nextLine();
        System.out.print("specify type of operation: add or remove ->");
        String action = scanner.nextLine();
        System.out.print("username ");
        String targetUser = scanner.nextLine();

        ChatRoom targetRoom = null;
        for (List<ChatRoom> chats : server.getUserChatsMap().values()) {
            for (ChatRoom room : chats) {
                if (room.isGroup() && room.getId().equals(groupId)) {
                    targetRoom = room;
                    break;
                }
            }
        }

        if (targetRoom == null) {
            System.out.println("the group not found!");
            return;
        }

        if ("add".equalsIgnoreCase(action)) {
            if (!server.getRegisteredMap().containsKey(targetUser)) {
                System.out.println("this user hasn't registered on system!");
                return;
            }
            server.addMemberToGroup(targetUser, groupId, targetUser);
            System.out.println("add the user successfully!");
        } else if ("remove".equalsIgnoreCase(action)) {
            targetRoom.removeMember(targetUser);
            // همچنین باید چت گروه را از مپ شخصی آن کاربر هم حذف کنیم
            List<ChatRoom> userChats = server.getUserChatsMap().get(targetUser);
            if (userChats != null)
                userChats.removeIf(r -> r.getId().equals(groupId));
            System.out.println("the user remove the group successfully!");
        }
    }
}
