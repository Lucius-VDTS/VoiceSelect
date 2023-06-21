package ca.vdts.voiceselect.files;


import static ca.vdts.voiceselect.database.entities.ColumnValue.COLUMN_VALUE_NONE;
import static ca.vdts.voiceselect.database.entities.SessionLayout.SESSION_LAYOUT_NONE;
import static ca.vdts.voiceselect.library.VDTSApplication.DEFAULT_UID;
import static ca.vdts.voiceselect.library.VDTSApplication.EXPORT_FILE_LAYOUT;
import static ca.vdts.voiceselect.library.VDTSApplication.EXPORT_FILE_OPTIONS;
import static ca.vdts.voiceselect.library.VDTSApplication.EXPORT_FILE_SETUP;
import static ca.vdts.voiceselect.library.VDTSApplication.EXPORT_FILE_USERS;
import static ca.vdts.voiceselect.library.VDTSApplication.FILE_EXTENSION_CSV;
import static ca.vdts.voiceselect.library.VDTSApplication.FILE_EXTENSION_JSON;
import static ca.vdts.voiceselect.library.VDTSApplication.FILE_EXTENSION_VDTS;
import static ca.vdts.voiceselect.library.VDTSApplication.FILE_EXTENSION_XLSX;
import static ca.vdts.voiceselect.library.VDTSApplication.SESSIONS_DIRECTORY;
import static ca.vdts.voiceselect.library.VDTSApplication.CONFIG_DIRECTORY;
import static ca.vdts.voiceselect.library.database.entities.VDTSUser.VDTS_USER_NONE;
import static ca.vdts.voiceselect.library.utilities.VDTSToolUtil.isNumeric;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.database.entities.SessionLayout;
import ca.vdts.voiceselect.files.JSONEntities.JSONColumnLayout;
import ca.vdts.voiceselect.files.JSONEntities.Options;
import ca.vdts.voiceselect.files.JSONEntities.TotalSession;
import ca.vdts.voiceselect.files.JSONEntities.Setup;
import ca.vdts.voiceselect.files.JSONEntities.Users;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.LocalDateTimeSerializer;
import ca.vdts.voiceselect.library.utilities.VDTSToolUtil;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class Exporter {
    private static final Logger LOG = LoggerFactory.getLogger(Exporter.class);
    private final static String FONT_NAME = "Arial";
    private static final int WIDTH_FACTOR = 256;
    private static final int BLANK_CELL = -987654321;

    private final DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern(
            "yyyyMMdd HHmmss"
    );
    private final DateTimeFormatter exportDateFormatter = DateTimeFormatter.ofPattern(
            "yyyy/MM/dd HH:mm:ss"
    );

    private final VSViewModel viewModel;
    private final VDTSApplication application;
    private final Activity activity;
    //private final ISaver saver;
    private final Gson gson;

    public Exporter(VSViewModel viewModel, VDTSApplication application, Activity activity
                    /*ISaver saver*/) {
        this.viewModel = viewModel;
        this.application = application;
        this.activity = activity;
        //this.saver = saver;
        gson = new GsonBuilder()
                .setLongSerializationPolicy(LongSerializationPolicy.STRING)
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
                .create();
    }

    public VSViewModel getViewModel() {
        return viewModel;
    }

    public VDTSApplication getApplication() {
        return application;
    }

    public Activity getActivity() {
        return activity;
    }

    /*public ISaver getSaver() {
        return saver;
    }*/

    public boolean exportUsers() {
        LOG.debug("Starting user export");
        final List<VDTSUser> userList = new ArrayList<>();
        final List<Column> columns = new ArrayList<>();
        final List<ColumnSpoken> columnSpoken = new ArrayList<>();
        final List<ColumnValue> values = new ArrayList<>();
        final List<ColumnValueSpoken> valueSpoken = new ArrayList<>();
        final Thread gatherThread = new Thread(() -> {
            LOG.debug("Starting db thread");
            userList.addAll(viewModel.findAllUsers());
            columns.addAll(viewModel.findAllColumns());
            columnSpoken.addAll(viewModel.findAllColumnSpokens());
            values.addAll(viewModel.findAllColumnValues());
            valueSpoken.addAll(viewModel.findAllColumnValueSpokens());
        });
        gatherThread.start();
        try {
            gatherThread.join();
        } catch (InterruptedException e) {
            LOG.error("exportUsers interrupted: ", e);
        }
        final Users users = new Users(
                userList,
                columns,
                columnSpoken,
                values,
                valueSpoken
        );
        final String json = gson.toJson(users);
        final String fileName = EXPORT_FILE_USERS
                .concat(FILE_EXTENSION_VDTS);

        return exportFile(json, CONFIG_DIRECTORY, fileName, false);
    }

    public boolean exportSetup() {
        LOG.debug("Starting setup export");
        final List<VDTSUser> users = new ArrayList<>();
        final List<Column> columns = new ArrayList<>();
        final List<ColumnSpoken> columnSpoken = new ArrayList<>();
        final List<ColumnValue> values = new ArrayList<>();
        final List<ColumnValueSpoken> valueSpoken = new ArrayList<>();
        final Thread gatherThread = new Thread(() -> {
            LOG.debug("Starting db thread");
            users.addAll(viewModel.findAllUsers());
            columns.addAll(viewModel.findAllColumns());
            final VDTSUser primary = viewModel.findPrimaryUser();
            columnSpoken.addAll(
                    viewModel.findAllColumnSpokensByUser(primary != null ? primary.getUid(): DEFAULT_UID)
            );
            values.addAll(viewModel.findAllColumnValues());
            valueSpoken.addAll(
                    viewModel.findAllColumnValueSpokensByUser(primary != null ? primary.getUid() : DEFAULT_UID)
            );

        });
        gatherThread.start();
        try {
            gatherThread.join();
        } catch (InterruptedException e) {
            LOG.error("exportSetup interrupted: ", e);
        }
        final Setup setup = new Setup(
                users,
                columns,
                columnSpoken,
                values,
                valueSpoken
        );
        final String json = gson.toJson(setup);
        final String fileName = EXPORT_FILE_SETUP
                .concat(FILE_EXTENSION_VDTS);

        return exportFile(json, CONFIG_DIRECTORY, fileName, false);
    }

    public boolean exportColumnLayout() {
        LOG.debug("Starting column layout export");
        final List<Layout> layouts = new ArrayList<>();
        final List<LayoutColumn> layoutColumns = new ArrayList<>();
        final List<Column> columns = new ArrayList<>();
        final List<VDTSUser> users = new ArrayList<>();
        final Thread gatherThread = new Thread(() -> {
            LOG.debug("Starting db thread");
            layouts.addAll(viewModel.findAllLayouts());
            layoutColumns.addAll(viewModel.findAllLayoutColumns());
            columns.addAll(viewModel.findAllColumns());
            users.addAll(viewModel.findAllUsers());
        });
        gatherThread.start();
        try {
            gatherThread.join();
        } catch (InterruptedException e) {
            LOG.error("exportColumnLayout interrupted: ", e);
        }
        final JSONColumnLayout JSONColumnLayout = new JSONColumnLayout(layouts,layoutColumns,columns,users);
        final String json = gson.toJson(JSONColumnLayout);
        final String fileName = EXPORT_FILE_LAYOUT
                .concat(FILE_EXTENSION_VDTS);

        return exportFile(json, CONFIG_DIRECTORY, fileName, false);
    }

    public boolean exportOptions() {
        LOG.debug("Starting options export");
        final Options options = new Options(application);
        final String json = gson.toJson(options);
        final String fileName = EXPORT_FILE_OPTIONS
                .concat(FILE_EXTENSION_VDTS);

        return exportFile(json, CONFIG_DIRECTORY, fileName, false);
    }

    public boolean exportSessionJSON(Session session) {
        LOG.debug("Starting session JSON export");
        final List<VDTSUser> users = new ArrayList<>();
        final List<Entry> entries = new ArrayList<>();
        final List<EntryValue> entryValues = new ArrayList<>();
        final List<ColumnValue> columnValues = new ArrayList<>();
        final List<Column> columns = new ArrayList<>();
        //final List<PictureReference> pictureReferences = new ArrayList<>();
        final Thread gatherThread = new Thread(() -> {
            LOG.debug("Starting db thread");
            users.addAll(viewModel.findAllUsers());
            entries.addAll(viewModel.findAllEntriesBySession((session.getUid())));
            entryValues.addAll(viewModel.findAllEntryValuesBySession(session.getUid()));
            columnValues.addAll(viewModel.findAllColumnValuesBySession(session.getUid()));
            columns.addAll(viewModel.findAllColumnsBySession(session.getUid()));
            //pictureReferences.addAll(viewModel.pictureReferences(session.getUid()));
        });
        gatherThread.start();
        try {
            gatherThread.join();
        } catch (InterruptedException e) {
            LOG.error("exportSessionJSON interrupted: ", e);
        }
        final TotalSession totalSession = new TotalSession(
                users,
                session,
                entries,
                entryValues,
                columnValues,
                columns
        );
        final String json = gson.toJson(totalSession);
        String exportPath;
        /*if (pictureReferences.size() > 0) {
            exportPath = SESSIONS_DIRECTORY
                    .concat(session.name().replace("/", "-"))
                    .concat(File.separator);
        } else {*/
            exportPath = SESSIONS_DIRECTORY;
        //}
        final String fileName = session.name()
                .replace("/", "-")
                .concat(" - ")
                .concat(LocalDateTime.now().format(fileDateFormatter))
                .concat(FILE_EXTENSION_JSON);

        return exportFile(json, exportPath, fileName, true);
    }

    public boolean exportSessionCSV(Session  session) {
        LOG.debug("Starting session csv export");
        final List<VDTSUser> users = new ArrayList<>();
        final List<SessionLayout> sessionLayouts = new ArrayList<>();
        final List<Entry> entries = new ArrayList<>();
        final List<EntryValue> entryValues = new ArrayList<>();
        final List<ColumnValue> columnValues = new ArrayList<>();
        final List<Column> columns = new ArrayList<>();
        //final List<PictureReference> pictureReferences = new ArrayList<>();
        final Thread gatherThread = new Thread(() -> {
            LOG.debug("Starting db thread");
            users.addAll(viewModel.findAllUsers());
            sessionLayouts.addAll(viewModel.findAllSessionLayoutsBySession(session.getUid()));
            entries.addAll(viewModel.findAllEntriesBySession(session.getUid()));
            entryValues.addAll(viewModel.findAllEntryValuesBySession(session.getUid()));
            columnValues.addAll(viewModel.findAllColumnValuesBySession(session.getUid()));
            columns.addAll(viewModel.findAllColumnsBySession(session.getUid()));
            //pictureReferences.addAll(viewModel.pictureReferences(session.getUid()));
        });
        gatherThread.start();
        try {
            gatherThread.join();
        } catch (InterruptedException e) {
            LOG.error("exportSessionCSV interrupted: ", e);
        }

        columns.sort(
                Comparator.comparingInt(
                        column -> sessionLayouts.stream()
                                .filter(
                                        headerColumn ->
                                                headerColumn.getColumnID() ==
                                                        column.getUid()
                                ).findFirst()
                                .orElse(SESSION_LAYOUT_NONE)
                                .getColumnPosition()
                )
        );
        entries.sort(Comparator.comparing(Entry::getCreatedDate));

        final StringBuilder csv = new StringBuilder();
        csv.append(session.name()).append(",");
        csv.append(
                users.stream()
                        .filter(user -> user.getUid() == session.getUserID())
                        .findFirst()
                        .orElse(VDTS_USER_NONE)
                        .getExportCode()
        ).append(",");
        csv.append(exportDateFormatter.format(session.getStartDate())).append(",");
        csv.append(
                session.getEndDate() != null ?
                        exportDateFormatter.format(session.getEndDate()) :
                        ""
        ).append("\n");
        csv.append("User").append(",");
        csv.append("Time Stamp").append(",");
        csv.append("Latitude").append(",");
        csv.append("Longitude").append(",");
        columns.forEach(column -> csv.append(column.getExportCode()).append(","));
        csv.replace(csv.lastIndexOf(","), csv.length(), "\n");
        entries.forEach(entry -> {
            final List<EntryValue> entryValueList = entryValues.stream()
                    .filter(entryValue -> entryValue.getEntryID() == entry.getUid())
                    .collect(Collectors.toList());
            csv.append(
                    users.stream()
                            .filter(user -> user.getUid() == entry.getUserID())
                            .findFirst()
                            .orElse(VDTS_USER_NONE)
                            .getExportCode()
            ).append(",");
            csv.append(exportDateFormatter.format(entry.getCreatedDate())).append(",");
            csv.append(entry.getLatitude() != null ? entry.getLatitude() : "").append(",");
            csv.append(entry.getLongitude() != null ? entry.getLongitude() : "").append(",");
            columns.forEach(
                    column -> csv
                            .append(
                                    columnValues.stream()
                                            .filter(
                                                    columnValue ->
                                                            columnValue.getColumnID() ==
                                                                    column.getUid()
                                            ).filter(
                                                    columnValue ->
                                                            entryValueList.stream()
                                                                    .anyMatch(
                                                                            entryValue ->
                                                                                    entryValue.getColumnValueID() ==
                                                                                            columnValue.getUid()
                                                                    )
                                            ).findFirst()
                                            .orElse(COLUMN_VALUE_NONE)
                                            .getExportCode()
                            ).append(",")
            );
            csv.replace(csv.lastIndexOf(","), csv.length(), "\n");
        });

        String exportPath;
        /*if (pictureReferences.size() > 0) {
            exportPath = SESSIONS_DIRECTORY
                    .concat(session.name().replace("/", "-"))
                    .concat(File.separator);
        } else {*/
            exportPath = SESSIONS_DIRECTORY;
        //}
        final String fileName = session.name()
                .replace("/", "-")
                .concat(" - ")
                .concat(LocalDateTime.now().format(fileDateFormatter))
                .concat(FILE_EXTENSION_CSV);

        return exportFile(csv.toString(), exportPath, fileName, true);
    }

    public boolean exportSessionExcel(Session  session) {
        LOG.debug("Starting session excel export");
        final List<VDTSUser> users = new ArrayList<>();
        final List<SessionLayout> sessionLayouts = new ArrayList<>();
        final List<Entry> entries = new ArrayList<>();
        final List<EntryValue> entryValues = new ArrayList<>();
        final List<ColumnValue> columnValues = new ArrayList<>();
        final List<Column> columns = new ArrayList<>();
        //final List<PictureReference> pictureReferences = new ArrayList<>();
        //final List<VideoReference> videoReferences = new ArrayList<>();
        final Thread gatherThread = new Thread(() -> {
            LOG.debug("Starting db thread");
            users.addAll(viewModel.findAllUsers());
            sessionLayouts.addAll(viewModel.findAllSessionLayoutsBySession(session.getUid()));
            entries.addAll(viewModel.findAllEntriesBySession(session.getUid()));
            entryValues.addAll(viewModel.findAllEntryValuesBySession(session.getUid()));
            columnValues.addAll(viewModel.findAllColumnValuesBySession(session.getUid()));
            columns.addAll(viewModel.findAllColumnsBySession(session.getUid()));
            //pictureReferences.addAll(viewModel.pictureReferences(session.getUid()));
            //videoReferences.addAll(viewModel.videoReferences(session.getUid()));
        });
        gatherThread.start();
        try {
            gatherThread.join();
        } catch (InterruptedException e) {
            LOG.error("exportSessionExcel interrupted: ", e);
        }

        columns.sort(
                Comparator.comparingInt(
                        column -> sessionLayouts.stream()
                                .filter(
                                        headerColumn ->
                                                headerColumn.getColumnID() ==
                                                        column.getUid()
                                ).findFirst()
                                .orElse(SESSION_LAYOUT_NONE)
                                .getColumnPosition()
                )
        );
        entries.sort(Comparator.comparing(Entry::getCreatedDate));

        final Workbook workbook = new XSSFWorkbook();
        final List<Object[]> data = new ArrayList<>();
        data.add(
                new Object[]{
                        session.name(),
                        users.stream()
                                .filter(user -> user.getUid() == session.getUserID())
                                .findFirst()
                                .orElse(VDTS_USER_NONE)
                                .getExportCode(),
                        exportDateFormatter.format(session.getStartDate()),
                        session.getEndDate() != null ?
                                exportDateFormatter.format(session.getEndDate()) :
                                ""
                }
        );
        final Object[] headerRow = new Object[columns.size() + 4];
        headerRow[0] = "User";
        headerRow[1] = "Time Stamp";
        headerRow[2] = "Latitude";
        headerRow[3] = "Longitude";
        final int[] index = {4};
        columns.forEach(column -> {
            headerRow[index[0]] = column.getExportCode();
            index[0]++;
        });
        data.add(headerRow);

        entries.forEach(entry -> {
            final List<EntryValue> entryValueList = entryValues.stream()
                    .filter(entryValue -> entryValue.getEntryID() == entry.getUid())
                    .collect(Collectors.toList());
            final Object[] row = new Object[columns.size() + 4];
            row[0] = users.stream()
                    .filter(user -> user.getUid() == entry.getUserID())
                    .findFirst()
                    .orElse(VDTS_USER_NONE)
                    .getExportCode();
            row[1] = exportDateFormatter.format(entry.getCreatedDate());
            row[2] = String.format(
                    Locale.CANADA,
                    "%f",
                    entry.getLatitude() != null ? entry.getLatitude() : null
            );
            row[3] = String.format(
                    Locale.CANADA,
                    "%f",
                    entry.getLongitude() != null ? entry.getLongitude() : null
            );
            index[0] = 4;
            columns.forEach(
                    column -> {
                        row[index[0]] = columnValues.stream()
                                .filter(
                                        columnValue -> columnValue.getColumnID() == column.getUid()
                                ).filter(
                                        columnValue ->
                                                entryValueList.stream()
                                                        .anyMatch(
                                                                entryValue ->
                                                                        entryValue.getColumnValueID() ==
                                                                                columnValue.getUid()
                                                        )
                                ).findFirst()
                                .orElse(COLUMN_VALUE_NONE)
                                .getExportCode();
                        index[0]++;
                    }
            );
            data.add(row);
        });
        final Sheet sheet = workbook.createSheet();
        sheet.setDefaultRowHeightInPoints(15);
        final Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints((short) 12);
        font.setBold(false);
        for (int i = 0; i < columns.size() + 1; i++) {
            sheet.setColumnWidth(i, 15 * WIDTH_FACTOR);
        }
        int rowNum = 0;
        for (Object[] datum : data) {
            final Row row = getRow(rowNum, sheet);

            int cellNum = 0;
            for(Object object : datum) {
                final Cell cell = getCell(cellNum, row);
                final String value = (String) object;

                if (isNumeric(value)) {
                    cell.setCellValue(Double.parseDouble(value));
                } else {
                    if (value.length() > 0 && value.charAt(0) == '=') {
                        cell.setCellFormula(value.substring(1));
                    } else if (value.equalsIgnoreCase("True")){
                        cell.setCellValue(true);
                    } else if (value.equalsIgnoreCase("False")){
                        cell.setCellValue(false);
                    } else {
                        cell.setCellValue(value);
                    }
                }
                cellNum++;
            }
            rowNum++;
        }
        workbook.setActiveSheet(0);
        workbook.setForceFormulaRecalculation(true);

        String exportPath;
        /*if (pictureReferences.size() > 0 || videoReferences.size() > 0) {
            exportPath = SESSIONS_DIRECTORY
                    .concat(session.name().replace("/", "-"))
                    .concat(File.separator);
        } else {*/
            exportPath = SESSIONS_DIRECTORY;
        //}
        final String fileName = session.name()
                .replace("/", "-")
                .concat(" - ")
                .concat(LocalDateTime.now().format(fileDateFormatter))
                .concat(FILE_EXTENSION_XLSX);

        return exportFile(workbook, exportPath, fileName);
    }

    private static Row getRow(int rowNum, Sheet sheet) {
        Row row = sheet.getRow(rowNum);
        if(row == null) {
            row = sheet.createRow(rowNum);
        }
        return row;
    }

    private static Cell getCell(int col, Row row) {
        Cell cell = row.getCell(col);
        if(cell == null) {
            cell = row.createCell(col);
        }
        return cell;
    }

    private boolean exportFile(String fileContent, String localExportPath, String fileName,
                               boolean isSession) {
        LOG.debug("Exporting file {}", fileName);
        String exportDir = Environment.getExternalStorageDirectory().toString() +
                "/Documents/VoiceSelect"+localExportPath;

        final File directory = new File(exportDir);
        if (!directory.exists()) {
            boolean mkDirResult = directory.mkdirs();
            LOG.info("Created directory: {}",mkDirResult);
            if (!mkDirResult) {
                LOG.info("Failed to create directory: {}", directory);
            }
        }

        try {
            writeToFile(exportDir.concat(localExportPath).concat(fileName), fileContent);
        } catch (IOException e) {
            LOG.error("Export file write failed: ", e);
            return false;
        }

        //TODO: MAKE WORK
        /*
        if (application.getPreferences().getBoolean(PREF_ONE_DRIVE, false)) {
            if (!isSession || localExportPath.equals(SESSIONS_DIRECTORY)) {
                exportToOneDrive(localExportPath, fileName);
            } else {
                exportToOneDrive(
                        SESSIONS_DIRECTORY,
                        localExportPath.substring(
                                SESSIONS_DIRECTORY.length() - 1,
                                localExportPath.length() - 1
                        )
                );
            }
        }*/
        return true;
    }

    private boolean exportFile(Workbook workbook, String localExportPath, String fileName) {
        final File directory = new File(localExportPath);
        //noinspection ResultOfMethodCallIgnored
        directory.mkdirs();

        try {
            writeToFile(localExportPath.concat(fileName), workbook);
        } catch (IOException e) {
            LOG.error("Users export file write failed: ", e);
            return false;
        }

        //TODO: MAKE WORK
        /*
        if (application.getPreferences().getBoolean(PREF_ONE_DRIVE, false)) {
            if (localExportPath.equals(SESSIONS_DIRECTORY)) {
                exportToOneDrive(localExportPath, fileName);
            } else {
                exportToOneDrive(
                        SESSIONS_DIRECTORY,
                        localExportPath.substring(
                                SESSIONS_DIRECTORY.length() - 1,
                                localExportPath.length() - 1
                        )
                );
            }
        }*/
        return true;
    }

    private void writeToFile(String filePath, String json) throws IOException {
        LOG.debug("Writing file {}", filePath);
        File file = new File(filePath);

        if (file.exists()) {
            boolean deleteResult = file.delete();
            if (deleteResult) {
                LOG.info("Deleted existing file(s): {}", file);
            } else {
                LOG.info("Failed to delete existing file: {}", file);
            }
        }

        //Create placeholder file for backup
        if (!file.exists()) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
                fos.write(json.getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*try {
            int bufferSize = 8192;
            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            InputStream dbInput = Files.newInputStream(db.toPath());
            OutputStream dbOutput = Files.newOutputStream(file.toPath());

            while ((bytesRead = dbInput.read(buffer, 0, bufferSize)) > 0) {
                dbOutput.write(buffer, 0, bytesRead);
            }

            dbOutput.flush();
            dbOutput.close();
            dbInput.close();

            backupSharedPreferences.edit().putLong(
                    "LAST_BACKUP",
                    VDTSToolUtil.getTimeStamp().getTime()).apply();

            LOG.info("Backup saved to {}", dbBackupDir);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("Backup error: ", e);
        }*/

        //noinspection ResultOfMethodCallIgnored
        /*file.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(json);
        writer.close();*/
    }

    private void writeToFile(String filePath, Workbook workbook) throws IOException {
        final File file = new File(filePath);
        final FileOutputStream fos = new FileOutputStream(file);
        workbook.write(fos);
        fos.flush();
        fos.close();
        workbook.close();
        activity.getApplication().sendBroadcast(
                new Intent(
                        Intent.ACTION_MEDIA_SCANNER_FINISHED,
                        Uri.fromFile(file)
                )
        );
    }

    //TODO: MAKE WORK
    /*
    public boolean exportMedia(Session  session) {
        return exportPhotos(session) && exportVideos(session);
    }*/

    //TODO: MAKE WORK
    /*private boolean exportPhotos(Session  session) {
        LOG.debug("Exporting photos");
        final List<Entry> entries = new ArrayList<>();
        final List<PictureReference> references = new ArrayList<>();
        final List<PictureReference> missing = new ArrayList<>();
        final Thread entryGatherThread = new Thread(() -> {
            LOG.debug("Starting entry db thread");
            entries.addAll(viewModel.findAllEntriesBySession(session.getUid()));
            references.addAll(viewModel.pictureReferences(session.getUid()));
        });
        entryGatherThread.start();
        try {
            entryGatherThread.join();
        } catch (InterruptedException e) {
            LOG.error("exportMedia interrupted: ", e);
            return false;
        }

        AtomicBoolean success = new AtomicBoolean(true);
        if (references.size() > 0) {
            String localExportPath = SESSIONS_DIRECTORY
                    .concat(session.name().replace("/", "-"))
                    .concat(File.separator)
                    .concat("Images")
                    .concat(File.separator);

            final File directory = new File(localExportPath);
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();

            entries.sort(Comparator.comparing(Entry::getCreatedDate));
            entries.forEach(entry -> {
                final List<PictureReference> entryReferences = references.stream()
                        .filter(reference -> reference.getEntryID() == entry.getUid())
                        .sorted(Comparator.comparing(PictureReference::getTimeStamp))
                        .collect(Collectors.toList());

                if (entryReferences.size() > 0) {
                    entryReferences.forEach(reference -> {
                        final int entryIndex = entries.indexOf(entry) + 1;
                        final int pictureIndex = entryReferences.indexOf(reference) + 1;
                        final File source = new File(reference.getPath());

                        if (source.exists()) {
                            final String photoName = session.name()
                                    .replace("/", "-")
                                    .concat(
                                            String.format(
                                                    Locale.CANADA,
                                                    " - %d - %d",
                                                    entryIndex,
                                                    pictureIndex
                                            )
                                    );
                            final String exportName = photoName
                                    .concat(" - ")
                                    .concat(fileDateFormatter.format(reference.getTimeStamp()))
                                    .concat(FILE_EXTENSION_JPG);
                            final String photoTime = exportDateFormatter.format(
                                    reference.getTimeStamp()
                            );
                            final String photoCoordinates = String.format(
                                    Locale.CANADA,
                                    "Lat: %f, Long: %f",
                                    reference.getLatitude(),
                                    reference.getLongitude()
                            );
                            final File destination = new File(localExportPath, exportName);

                            try {
                                copy(source, destination);
                                LOG.info(
                                        "File {} copied to {}",
                                        source.getPath(),
                                        destination.getPath()
                                );
                                activity.getApplication()
                                        .sendBroadcast(
                                                new Intent(
                                                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                        Uri.fromFile(destination)
                                                )
                                        );
                            } catch (IOException e) {
                                LOG.error(
                                        "Exception copying {} to {}: ",
                                        source.getPath(),
                                        destination.getPath(),
                                        e
                                );
                                success.set(false);
                            }
                            try {
                                ImageFileUtils.drawToBitmap(
                                        destination.getPath(),
                                        application.getPreferences().getBoolean(
                                                PREF_PHOTO_PRINT_NAME,
                                                false
                                        ) ?
                                                photoName :
                                                "",
                                        application.getPreferences().getBoolean(
                                                PREF_PHOTO_PRINT_TIME,
                                                false
                                        ) ?
                                                photoTime :
                                                "",
                                        application.getPreferences().getBoolean(
                                                PREF_PHOTO_PRINT_GPS,
                                                false
                                        ) ?
                                                photoCoordinates :
                                                ""
                                );
                            } catch (IOException e) {
                                LOG.error("Exception drawing to bitmap: ", e);
                                success.set(false);
                            }
                        } else {
                            LOG.info(
                                    "No file found at {}, the user may have deleted it",
                                    source.getPath()
                            );
                            missing.add(reference);
                        }
                    });
                }
            });

            missing.forEach(
                    pictureReference -> new Thread(() -> viewModel.delete(pictureReference)).start()
            );
        }
        return success.get();
    }*/

    //TODO: MAKE WORK
    /*private boolean exportVideos(Session  session) {
        LOG.debug("Exporting videos");
        final List<VideoReference> references = new ArrayList<>();
        final List<VideoReference> missing = new ArrayList<>();
        final Thread gatherThread = new Thread(() -> {
            LOG.debug("Starting reference db thread");
            references.addAll(viewModel.videoReferences(session.getUid()));
        });
        gatherThread.start();
        try {
            gatherThread.join();
        } catch (InterruptedException e) {
            LOG.error("exportMedia interrupted: ", e);
            return false;
        }

        AtomicBoolean success = new AtomicBoolean(true);
        if (references.size() > 0) {
            String localExportPath = SESSIONS_DIRECTORY
                    .concat(session.name().replace("/", "-"))
                    .concat(File.separator)
                    .concat("Videos")
                    .concat(File.separator);

            final File directory = new File(localExportPath);
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();

            final List<VideoReference> sortedReferences = references.stream()
                    .sorted(Comparator.comparing(VideoReference::getTimeStamp))
                    .collect(Collectors.toList());

            if (sortedReferences.size() > 0) {
                sortedReferences.forEach(reference -> {
                    final int videoIndex = sortedReferences.indexOf(reference) + 1;
                    final File source = new File(reference.getPath());

                    if (source.exists()) {
                        final String videoName = session.name()
                                .replace("/", "-")
                                .concat(String.format(Locale.CANADA, " - %d", videoIndex));
                        final String exportName = videoName
                                .concat(" - ")
                                .concat(fileDateFormatter.format(reference.getTimeStamp()))
                                .concat(FILE_EXTENSION_MP4);
                        final File destination = new File(localExportPath, exportName);

                        try {
                            copy(source, destination);
                            LOG.info(
                                    "File {} copied to {}",
                                    source.getPath(),
                                    destination.getPath()
                            );
                            activity.getApplication()
                                    .sendBroadcast(
                                            new Intent(
                                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                    Uri.fromFile(destination)
                                            )
                                    );
                        } catch (IOException e) {
                            LOG.error(
                                    "Exception copying {} to {}: ",
                                    source.getPath(),
                                    destination.getPath(),
                                    e
                            );
                            success.set(false);
                        }
                    } else {
                        LOG.info(
                                "No file found at {}, the user may have deleted it",
                                source.getPath()
                        );
                        missing.add(reference);
                    }
                });
            }

            missing.forEach(
                    videoReference -> new Thread(() -> viewModel.delete(videoReference)).start()
            );
        }
        return success.get();
    }*/

    private void copy(File source, File destination) throws IOException {
        final FileInputStream inStream = new FileInputStream(source);
        final FileOutputStream outStream = new FileOutputStream(destination);
        final FileChannel inChannel = inStream.getChannel();
        final FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    //TODO: MAKE WORK
    /*private void exportToOneDrive(String localExportPath, String fileName) {
        final File file = new File(localExportPath, fileName);
        saver.setRequestCode(ONEDRIVE_REQUEST_CODE);
        saver.startSaving(activity, fileName, Uri.fromFile(file));
    }*/
}
