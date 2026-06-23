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
        System.out.println("\n=== سیستم خط فرمان (CLI) مدیریت ادمین بیدار شد ===");

        // مرحله احراز هویت ادمین
        while (!loggedIn) {
            System.out.print("نام کاربری ادمین: ");
            String user = scanner.nextLine();
            System.out.print("رمز عبور ادمین: ");
            String pass = scanner.nextLine();

            if (ADMIN_USER.equals(user) && ADMIN_PASS.equals(pass)) {
                loggedIn = true;
                System.out.println("ورود موفقیت‌آمیز بود! به پنل مدیریت خوش آمدید.");
            } else {
                System.out.println("رمز عبور یا نام کاربری اشتباه است. دوباره تلاش کنید.");
            }
        }

        while (loggedIn) {
            printMenu();
            System.out.print("👉 لطفاً یک گزینه را انتخاب کنید (1-9): ");
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
                        System.out.println("از پنل ادمین خارج شدید.");
                        break;
                    default:
                        System.out.println("گزینه نامعتبر! عددی بین ۱ تا ۹ وارد کنید.");
                }
            } catch (Exception e) {
                System.out.println("⚠️ خطایی در اجرای دستور رخ داد: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n======= 🛠 پنل مدیریت مرکزی (CLI) =======");
        System.out.println("1. نمایش لیست تمام کاربران سیستم");
        System.out.println("2. اضافه کردن کاربر جدید (دستی)");
        System.out.println("3. حذف کامل یک کاربر از سیستم");
        System.out.println("4. نمایش لیست گروه‌ها و اعضای آن‌ها");
        System.out.println("5. ایجاد یک گروه جدید (دستی)");
        System.out.println("6. حذف کامل یک گروه");
        System.out.println("7. مدیریت اعضای گروه (اضافه/حذف عضو)");
        System.out.println("8. مشاهده پیام‌های گزارش شده (Reports)");
        System.out.println("9. خروج از پنل ادمین (Logout)");
        System.out.println("=========================================");
    }

    // نمایش لیست کاربران
    private void showAllUsers() {
        System.out.println("\n--- لیست کاربران ثبت شده ---");
        Map<String, User> users = server.getRegisteredMap();
        if (users.isEmpty()) {
            System.out.println("هیچ کاربری در سیستم ثبت‌نام نکرده است.");
            return;
        }
        for (User u : users.values()) {
            System.out.printf("👤 نام کاربری: %s | آیدی نمایشی: %s\n", u.getUsername(), u.getID());
        }
    }

    // اضافه کردن دستی کاربر
    private void addUserManually() {
        System.out.println("\n--- ثبت‌نام دستی کاربر جدید توسط ادمین ---");
        System.out.print("نام کاربری جدید: ");
        String user = scanner.nextLine();
        System.out.print("رمز عبور: ");
        String pass = scanner.nextLine();
        System.out.print("ID: ");
        String ID = scanner.next();

        if (user.isEmpty() || pass.isEmpty()) {
            System.out.println("نام کاربری یا رمز عبور نمی‌تواند خالی باشد.");
            return;
        }

        SignupResult res = server.register(user, ID, pass, pass);
        if (res == SignupResult.SUCCESS) {
            System.out.println("کاربر با موفقیت به سیستم اضافه شد.");
        } else {
            System.out.println("خطای سیستم: این نام کاربری تکراری است.");
        }
    }

    private void deleteUserManually() {
        System.out.print("\nنام کاربری که می‌خواهید حذف شود را وارد کنید: ");
        String user = scanner.nextLine();
        boolean ok = server.deleteUserAccount(user);
        if (ok)
            System.out.println("کاربر و تمام چت‌هایش با موفقیت برای همیشه پاک شدند.");
        else
            System.out.println("چنین کاربری در سیستم پیدا نشد.");
    }

    private void showAllGroups() {
        System.out.println("\n--- لیست گروه‌های موجود و اعضای آن‌ها ---");
        Set<String> printedGroupIds = new HashSet<>();
        int count = 0;

        for (List<ChatRoom> chats : server.getUserChatsMap().values()) {
            for (ChatRoom room : chats) {
                if (room.isGroup() && !printedGroupIds.contains(room.getId())) {
                    printedGroupIds.add(room.getId());
                    count++;
                    System.out.printf("[%d] نام گروه: %s | آیدی گروه: %s\n", count, room.getName(), room.getId());
                    System.out.println("اعضا: " + room.getMembers());
                }
            }
        }
        if (count == 0)
            System.out.println("هیچ گروهی در سیستم ثبت نشده است.");
    }

    private void createGroupManually() {
        System.out.print("\nنام گروه جدید: ");
        String groupName = scanner.nextLine();
        if (groupName.isEmpty())
            return;

        boolean ok = server.createNewGroup("admin", groupName, new ArrayList<>());
        if (ok)
            System.out.println("گروه با موفقیت ساخته شد.");
    }

    private void deleteGroupManually() {
        System.out.print("\nآیدی گروهی که می‌خواهید حذف شود را وارد کنید (نمونه group_171...): ");
        String groupId = scanner.nextLine();
        boolean ok = server.adminDeleteGroup(groupId);
        if (ok)
            System.out.println("گروه با موفقیت منحل و از چت تمام اعضا پاک شد.");
        else
            System.out.println("گروهی با این آیدی یافت نشد.");
    }

    private void manageGroupMembers() {
        System.out.print("\nآیدی گروه مورد نظر: ");
        String groupId = scanner.nextLine();
        System.out.print("نوع عملیات را مشخص کنید (add برای اضافه کردن / remove برای حذف): ");
        String action = scanner.nextLine();
        System.out.print("نام کاربری شخص مورد نظر: ");
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
            System.out.println("گروه پیدا نشد.");
            return;
        }

        if ("add".equalsIgnoreCase(action)) {
            if (!server.getRegisteredMap().containsKey(targetUser)) {
                System.out.println("این کاربر اصلاً در سیستم ثبت نام نکرده است.");
                return;
            }
            server.addMemberToGroup(targetUser, groupId, targetUser);
            System.out.println("کاربر با موفقیت عضو گروه شد.");
        } else if ("remove".equalsIgnoreCase(action)) {
            targetRoom.removeMember(targetUser);
            // همچنین باید چت گروه را از مپ شخصی آن کاربر هم حذف کنیم
            List<ChatRoom> userChats = server.getUserChatsMap().get(targetUser);
            if (userChats != null)
                userChats.removeIf(r -> r.getId().equals(groupId));
            System.out.println("کاربر با موفقیت از گروه اخراج شد.");
        }
    }
}
