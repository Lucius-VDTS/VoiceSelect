package ca.vdts.voiceselect.files;


import static ca.vdts.voiceselect.database.entities.Column.COLUMN_NONE;
import static ca.vdts.voiceselect.library.VDTSApplication.DEFAULT_UID;
import static ca.vdts.voiceselect.library.database.entities.VDTSUser.VDTS_USER_NONE;

import android.app.Activity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.files.JSONEntities.ColumnWords;
import ca.vdts.voiceselect.files.JSONEntities.JSONColumnLayout;
import ca.vdts.voiceselect.files.JSONEntities.JSONLayout;
import ca.vdts.voiceselect.files.JSONEntities.JSONLayoutColumn;
import ca.vdts.voiceselect.files.JSONEntities.Options;
import ca.vdts.voiceselect.files.JSONEntities.Setup;
import ca.vdts.voiceselect.files.JSONEntities.Users;
import ca.vdts.voiceselect.files.JSONEntities.ValueWords;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSLocalDateTimeSerializerUtil;

public class Importer {
    private static final Logger LOG = LoggerFactory.getLogger(Importer.class);

    private final VSViewModel viewModel;
    private final VDTSApplication application;
    private final Activity activity;
    private final Gson gson;

    public Importer(VSViewModel viewModel, Activity activity, VDTSApplication application) {
        this.viewModel = viewModel;
        this.application = application;
        this.activity = activity;
        gson = new GsonBuilder()
                .setLongSerializationPolicy(LongSerializationPolicy.STRING)
                .registerTypeAdapter(LocalDateTime.class, new VDTSLocalDateTimeSerializerUtil())
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

    public Gson getGson() {
        return gson;
    }

    public boolean importUsers(File file) {
        LOG.debug("Starting users import");
        final String jsonString = getJsonString(file);
        final Type type = new TypeToken<Users>() {}.getType();
        if (jsonString != null) {
            if (jsonString.contains("Users")) {
                final Users users;
                try {
                    users = gson.fromJson(jsonString, type);
                } catch (JsonSyntaxException e) {
                    LOG.error("unable to parse jsonString: ", e);
                    return false;
                }
                if (users != null) {
                    final List<VDTSUser> currentUsers = new ArrayList<>();
                    final List<Column> currentColumns = new ArrayList<>();
                    final List<ColumnSpoken> currentColumnSpoken = new ArrayList<>();
                    final List<ColumnValue> currentColumnValues = new ArrayList<>();
                    final List<ColumnValueSpoken> currentValueSpoken = new ArrayList<>();
                    final Thread gatherThread = new Thread(() -> {
                        LOG.debug("Starting db thread");
                        currentUsers.addAll(viewModel.findAllUsers());
                        currentColumns.addAll(viewModel.findAllColumns());
                        currentColumnSpoken.addAll(viewModel.findAllColumnSpokens());
                        currentColumnValues.addAll(viewModel.findAllColumnValues());
                        currentValueSpoken.addAll(viewModel.findAllColumnValueSpokens());
                    });
                    gatherThread.start();
                    try {
                        gatherThread.join();
                    } catch (InterruptedException e) {
                        LOG.error("importUsers interrupted: ", e);
                        return false;
                    }

                    users.getUsers().forEach(user -> {
                        VDTSUser currentUser = currentUsers.stream()
                                .filter(
                                        cu -> cu.getExportCode().equalsIgnoreCase(
                                                user.getExportCode()
                                        )
                                )
                                .findFirst()
                                .orElse(null);
                        final long[] currentUserID = new long[1];
                        if (currentUser != null) {
                            currentUser.setName(user.getName());
                            currentUser.setExportCode(user.getExportCode());
                            currentUser.setInitials(user.getInitials());
                            currentUser.setSessionPrefix(user.getSessionPrefix());
                            currentUser.setAuthority(user.getAuthority());
                            currentUser.setPrimary(user.isPrimary());
                            currentUser.setPassword(user.getPassword());
                            currentUser.setAutosave(user.getAutosave());
                            currentUser.setAbbreviate(user.isAbbreviate());
                            currentUser.setFeedback(user.getFeedback());
                            currentUser.setFeedbackFlushQueue(user.isFeedbackQueue());
                            currentUser.setFeedbackRate(user.getFeedbackRate());
                            currentUser.setFeedbackPitch(user.getFeedbackPitch());
                            currentUser.setActive(user.isActive());
                            VDTSUser finalCurrentUser = currentUser;
                            new Thread(() -> viewModel.updateUser(finalCurrentUser)).start();
                            currentUserID[0] = currentUser.getUid();
                        } else {
                            currentUser = new VDTSUser(
                                    0,
                                    user.getName(),
                                    user.getExportCode(),
                                    user.getInitials(),
                                    user.getSessionPrefix(),
                                    user.getAuthority(),
                                    user.isPrimary(),
                                    user.getPassword(),
                                    user.getAutosave(),
                                    user.isAbbreviate(),
                                    user.getFeedback(),
                                    user.isFeedbackQueue(),
                                    user.getFeedbackRate(),
                                    user.getFeedbackPitch()
                            );
                            currentUser.setActive(user.isActive());
                            VDTSUser finalCurrentUser1 = currentUser;
                            final Thread userInsertThread = new Thread(
                                    () -> currentUserID[0] = viewModel.insertUser(finalCurrentUser1)
                            );
                            userInsertThread.start();
                            try {
                                userInsertThread.join();
                            } catch (InterruptedException e) {
                                LOG.error("user insert interrupted: ", e);
                            }
                            currentUser.setUid(currentUserID[0]);
                        }

                        VDTSUser currentPrimary = currentUsers.stream()
                                .filter(VDTSUser::isPrimary)
                                .findFirst()
                                .orElse(null);
                        if (currentPrimary != null) {
                            currentPrimary.setPrimary(user.isPrimary());
                            new Thread(() -> viewModel.updateUser(currentPrimary)).start();
                        } else {
                            currentUser.setPrimary(true);
                            VDTSUser finalCurrentUser2 = currentUser;
                            final Thread primaryInsertThread = new Thread(
                                    () -> viewModel.updateUser(finalCurrentUser2)
                            );
                            primaryInsertThread.start();
                            try {
                                primaryInsertThread.join();
                            } catch (InterruptedException e) {
                                LOG.error("primary insert interrupted: ", e);
                            }
                        }

                        final List<ColumnWords> columnWordList = user.getColumnWords();
                        if (columnWordList.size() > 0) {
                            currentColumns.forEach(currentColumn -> {
                                final ColumnWords columnWords = columnWordList.stream()
                                        .filter(
                                                columnWords1 -> columnWords1.getCode()
                                                        .equalsIgnoreCase(
                                                                currentColumn.getExportCode()
                                                        )
                                        ).findFirst()
                                        .orElse(null);
                                if (columnWords != null) {
                                    final List<ColumnSpoken> currentUserColumnSpoken =
                                            currentColumnSpoken.stream()
                                                    .filter(
                                                            columnSpoken ->
                                                                    columnSpoken.getUserID() ==
                                                                            currentUserID[0]
                                                    ).filter(
                                                            columnSpoken ->
                                                                    columnSpoken.getColumnID() ==
                                                                            currentColumn.getUid()
                                                    ).collect(Collectors.toList());
                                    columnWords.getWords().forEach(word -> {
                                        if (currentUserColumnSpoken.stream().noneMatch(columnSpoken -> columnSpoken.getSpoken().equalsIgnoreCase(word.getWord()))) {
                                            new Thread(
                                                    () -> viewModel.insertColumnSpoken(
                                                            new ColumnSpoken(
                                                                    currentUserID[0],
                                                                    currentColumn.getUid(),
                                                                    word.getWord()
                                                            )
                                                    )
                                            ).start();
                                        }
                                    });
                                    currentUserColumnSpoken.forEach(columnSpoken -> {
                                        if (columnWords.getWords().size() > 0 &&
                                                columnWords.getWords().stream().noneMatch(word -> word.getWord().equalsIgnoreCase(columnSpoken.getSpoken()))) {
                                            new Thread(
                                                    () -> viewModel.deleteColumnSpoken(columnSpoken)
                                            ).start();
                                        }
                                    });
                                }
                            });
                        }

                        final List<ValueWords> valueWordList = user.getValueWords();
                        if (valueWordList.size() > 0) {
                            currentColumns.forEach(currentColumn -> {
                                List<ColumnValue> currentColumnValueList =
                                        currentColumnValues.stream()
                                                .filter(
                                                        columnValue ->
                                                                columnValue.getColumnID() ==
                                                                        currentColumn.getUid()
                                                ).collect(Collectors.toList());
                                currentColumnValueList.forEach(currentColumnValue -> {
                                    final ValueWords valueWords = valueWordList.stream()
                                            .filter(
                                                    valueWords1 ->
                                                            valueWords1.getColumnCode()
                                                                    .equalsIgnoreCase(
                                                                            currentColumn
                                                                                    .getExportCode()
                                                                    )
                                            ).filter(
                                                    valueWords1 ->
                                                            valueWords1.getValueCode()
                                                                    .equalsIgnoreCase(
                                                                            currentColumnValue
                                                                                    .getExportCode()
                                                                    )
                                            ).findFirst()
                                            .orElse(null);
                                    if (valueWords != null) {
                                        final List<ColumnValueSpoken> currentUserValueSpoken =
                                                currentValueSpoken.stream()
                                                        .filter(
                                                                valueSpoken ->
                                                                        valueSpoken.getUserID() ==
                                                                                currentUserID[0]
                                                        ).filter(
                                                                valueSpoken ->
                                                                        valueSpoken.getColumnValueID() ==
                                                                                currentColumnValue.getUid()
                                                        ).collect(Collectors.toList());
                                        valueWords.getWords().forEach(word -> {
                                            if (currentUserValueSpoken.stream().noneMatch(valueSpoken -> valueSpoken.getSpoken().equalsIgnoreCase(word.getWord()))) {
                                                new Thread(
                                                        () -> viewModel.insertColumnValueSpoken(
                                                                new ColumnValueSpoken(
                                                                        currentUserID[0],
                                                                        currentColumnValue.getUid(),
                                                                        word.getWord()
                                                                )
                                                        )
                                                ).start();
                                            }
                                        });
                                        currentUserValueSpoken.forEach(valueSpoken -> {
                                            if (valueWords.getWords().size() > 0 &&
                                                    valueWords.getWords().stream().noneMatch(word -> word.getWord().equalsIgnoreCase(valueSpoken.getSpoken()))) {
                                                new Thread(() -> viewModel.deleteColumnValueSpoken(valueSpoken)).start();
                                            }
                                        });
                                    }
                                });
                            });
                        }
                    });

                    currentUsers.forEach(currentUser -> {
                        if (users.getUsers().stream().noneMatch(user -> user.getExportCode().equalsIgnoreCase(currentUser.getExportCode()))) {
                            VDTSUser currentPrimary = currentUsers.stream()
                                    .filter(
                                            userPrimary ->
                                                    userPrimary.getUid() == currentUser.getUid() &&
                                                            userPrimary.isPrimary()
                                    ).findFirst()
                                    .orElse(null);
                            if (currentPrimary != null) {
                                currentPrimary.setPrimary(false);
                                new Thread(() -> viewModel.updateUser(currentPrimary)).start();
                            }
                        }
                    });
                } else {
                    LOG.debug("users is null");
                    return false;
                }
            } else {
                LOG.debug("jsonString does not contain Users");
                return false;
            }
        } else {
            LOG.debug("jsonString is null");
            return false;
        }
        return true;
    }

    public boolean importSetup(File uri) {
        LOG.debug("Starting Setups import");
        final String jsonString = getJsonString(uri);
        final Type type = new TypeToken<Setup>() {}.getType();
        if (jsonString != null) {
            if (jsonString.contains("Columns") || jsonString.contains("Values")) {
                final Setup setup;
                try {
                    setup = gson.fromJson(jsonString, type);
                } catch (JsonSyntaxException e) {
                    LOG.error("unable to parse jsonString: ", e);
                    return false;
                }
                if (setup != null) {
                    final List<VDTSUser> currentUsers = new ArrayList<>();
                    final List<Column> currentColumns = new ArrayList<>();
                    final List<ColumnSpoken> currentColumnSpoken = new ArrayList<>();
                    final List<ColumnValue> currentColumnValues = new ArrayList<>();
                    final List<ColumnValueSpoken> currentValueSpoken = new ArrayList<>();
                    final Thread gatherThread = new Thread(() -> {
                        LOG.debug("Starting db thread");
                        currentUsers.addAll(viewModel.findAllUsers());
                        currentColumns.addAll(viewModel.findAllColumns());
                        currentColumnSpoken.addAll(viewModel.findAllColumnSpokens());
                        currentColumnValues.addAll(viewModel.findAllColumnValues());
                        currentValueSpoken.addAll(viewModel.findAllColumnValueSpokens());
                    });
                    gatherThread.start();
                    try {
                        gatherThread.join();
                    } catch (InterruptedException e) {
                        LOG.error("importSetup interrupted: ", e);
                        return false;
                    }

                    setup.getColumns().forEach(jsonColumn -> {
                        Column currentColumn = currentColumns.stream()
                                .filter(
                                        column -> column.getExportCode().equalsIgnoreCase(
                                                jsonColumn.getExportCode()
                                        )
                                ).findFirst()
                                .orElse(null);
                        final long[] currentColumnID = new long[1];
                        if (currentColumn != null) {
                            currentColumn.setUserID(
                                    currentUsers.stream()
                                            .filter(
                                                    user -> user.getExportCode().equalsIgnoreCase(
                                                            jsonColumn.getUserCode()
                                                    )
                                            ).findFirst()
                                            .orElse(VDTS_USER_NONE)
                                            .getUid()
                            );
                            currentColumn.setCreatedDate(jsonColumn.getCreateDate());
                            currentColumn.setName(jsonColumn.getDisplayName());
                            currentColumn.setNameCode(jsonColumn.getDisplayCode());
                            currentColumn.setExportCode(jsonColumn.getExportCode());
                            currentColumn.setActive(jsonColumn.isActive());
                            Column finalCurrentColumn1 = currentColumn;
                            new Thread(() -> viewModel.updateColumn(finalCurrentColumn1)).start();
                            currentColumnID[0] = currentColumn.getUid();
                        } else {
                            currentColumn = new Column(
                                    currentUsers.stream()
                                            .filter(
                                                    user -> user.getExportCode().equalsIgnoreCase(
                                                            jsonColumn.getUserCode()
                                                    )
                                            ).findFirst()
                                            .orElse(VDTS_USER_NONE)
                                            .getUid(),
                                    jsonColumn.getDisplayName(),
                                    jsonColumn.getDisplayCode(),
                                    jsonColumn.getExportCode()
                            );
                            currentColumn.setCreatedDate(jsonColumn.getCreateDate());
                            currentColumn.setActive(jsonColumn.isActive());
                            Column finalCurrentColumn = currentColumn;
                            final Thread columnInsertThread = new Thread(
                                    () -> currentColumnID[0] = viewModel.insertColumn(
                                            finalCurrentColumn
                                    )
                            );
                            columnInsertThread.start();
                            try {
                                columnInsertThread.join();
                            } catch (InterruptedException e) {
                                LOG.error("column insert interrupted: ", e);
                            }
                            currentColumn.setUid(currentColumnID[0]);
                            currentColumns.add(currentColumn);
                        }

                        currentUsers.forEach(currentUser -> {
                            final List<ColumnSpoken> currentUserColumnSpoken =
                                    currentColumnSpoken.stream()
                                            .filter(
                                                    columnSpoken -> columnSpoken.getColumnID() ==
                                                            currentColumnID[0]
                                            ).filter(
                                                    columnSpoken -> columnSpoken.getUserID() ==
                                                            currentUser.getUid()
                                            ).collect(Collectors.toList());
                            if (currentUserColumnSpoken.size() == 0) {
                                jsonColumn.getDefaultWords().forEach(
                                        word -> new Thread(
                                                () -> viewModel.insertColumnSpoken(
                                                        new ColumnSpoken(
                                                                currentUser.getUid(),
                                                                currentColumnID[0],
                                                                word.getWord()
                                                        )
                                                )
                                        ).start()
                                );
                            }
                        });
                    });

                    setup.getValues().forEach(jsonValue -> {
                        final long currentColumnID = currentColumns.stream()
                                .filter(
                                        column -> column.getExportCode().equalsIgnoreCase(
                                                jsonValue.getColumnCode()
                                        )
                                ).findFirst()
                                .orElse(COLUMN_NONE)
                                .getUid();
                        ColumnValue currentValue = currentColumnValues.stream()
                                .filter(columnValue -> columnValue.getColumnID() == currentColumnID)
                                .filter(
                                        columnValue -> columnValue.getExportCode().equalsIgnoreCase(
                                                jsonValue.getExportCode()
                                        )
                                ).findFirst()
                                .orElse(null);
                        final long[] currentValueID = new long[1];
                        if (currentValue != null) {
                            currentValue.setUserID(
                                    currentUsers.stream()
                                            .filter(
                                                    user -> user.getExportCode().equalsIgnoreCase(
                                                            jsonValue.getUserCode()
                                                    )
                                            ).findFirst()
                                            .orElse(VDTS_USER_NONE)
                                            .getUid()
                            );
                            currentValue.setColumnID(currentColumnID);
                            currentValue.setCreatedDate(jsonValue.getCreateDate());
                            currentValue.setName(jsonValue.getDisplayName());
                            currentValue.setNameCode(jsonValue.getDisplayCode());
                            currentValue.setExportCode(jsonValue.getExportCode());
                            currentValue.setActive(jsonValue.isActive());
                            ColumnValue finalCurrentValue = currentValue;
                            new Thread(
                                    () -> viewModel.updateColumnValue(finalCurrentValue)
                            ).start();
                            currentValueID[0] = currentValue.getUid();
                        } else {
                            currentValue = new ColumnValue(
                                    currentUsers.stream()
                                            .filter(
                                                    user -> user.getExportCode().equalsIgnoreCase(
                                                            jsonValue.getUserCode()
                                                    )
                                            ).findFirst()
                                            .orElse(VDTS_USER_NONE)
                                            .getUid(),
                                    currentColumnID,
                                    jsonValue.getDisplayName(),
                                    jsonValue.getDisplayCode(),
                                    jsonValue.getExportCode()
                            );
                            currentValue.setCreatedDate(jsonValue.getCreateDate());
                            currentValue.setActive(jsonValue.isActive());
                            ColumnValue finalCurrentValue1 = currentValue;
                            final Thread valueInsertThread = new Thread(
                                    () -> currentValueID[0] = viewModel.insertColumnValue(
                                            finalCurrentValue1
                                    )
                            );
                            valueInsertThread.start();
                            try {
                                valueInsertThread.join();
                            } catch (InterruptedException e) {
                                LOG.error("value insert interrupted: ", e);
                            }
                            currentValue.setUid(currentValueID[0]);
                        }

                        currentUsers.forEach(currentUser -> {
                            final List<ColumnValueSpoken> currentUserValueSpoken =
                                    currentValueSpoken.stream()
                                            .filter(
                                                    valueSpoken -> valueSpoken.getColumnValueID() ==
                                                            currentValueID[0]
                                            ).filter(
                                                    valueSpoken -> valueSpoken.getUserID() ==
                                                            currentUser.getUid()
                                            ).collect(Collectors.toList());
                            if (currentUserValueSpoken.size() == 0) {
                                jsonValue.getDefaultWords().forEach(
                                        word -> new Thread(
                                                () -> viewModel.insertColumnValueSpoken(
                                                        new ColumnValueSpoken(
                                                                currentUser.getUid(),
                                                                currentValueID[0],
                                                                word.getWord()
                                                        )
                                                )
                                        ).start()
                                );
                            }
                        });
                    });
                } else {
                    LOG.debug("setup is null");
                    return false;
                }
            } else {
                LOG.debug("jsonString does not contain Columns or Values");
                return false;
            }
        } else {
            LOG.debug("jsonString is null");
            return false;
        }
        return true;
    }

    public boolean importColumnLayout(File uri) {
        LOG.debug("Starting column layout import");
        final String jsonString = getJsonString(uri);
        final Type type = new TypeToken<JSONColumnLayout>() {}.getType();
        if (jsonString != null) {
            if (jsonString.contains("Layouts")) {
                final JSONColumnLayout JSONColumnLayout;
                try {
                    JSONColumnLayout = gson.fromJson(jsonString, type);
                } catch (JsonSyntaxException e) {
                    LOG.error("unable to parse jsonString: ", e);
                    return false;
                }
                if (JSONColumnLayout != null && JSONColumnLayout.getLayouts().size() > 0) {
                    final List<Layout> currentLayouts = new ArrayList<>();
                    final List<Column> currentColumns = new ArrayList<>();
                    final List<VDTSUser> currentUsers = new ArrayList<>();
                    final Thread gatherThread = new Thread(() -> {
                        LOG.debug("Starting db thread");
                        currentLayouts.addAll(viewModel.findAllLayouts());
                        currentColumns.addAll(viewModel.findAllColumns());
                        currentUsers.addAll(viewModel.findAllUsers());
                    });
                    gatherThread.start();
                    try {
                        gatherThread.join();
                    } catch (InterruptedException e) {
                        LOG.error("importColumnLayout interrupted: ", e);
                        return false;
                    }

                    for (JSONLayout layout : JSONColumnLayout.getLayouts()) {
                        Layout currentLayout = currentLayouts.stream()
                                .filter(
                                        l -> l.getExportCode()
                                                .equalsIgnoreCase(layout.getExportCode())
                                ).findFirst()
                                .orElse(null);
                        VDTSUser currentUser = currentUsers.stream()
                                .filter(
                                        l -> l.getExportCode().equalsIgnoreCase(
                                                layout.getUserCode()
                                        )
                                ).findFirst()
                                .orElse(null);
                        if (currentLayout == null) {
                            LOG.debug(
                                    "Found new layout with export code {}",
                                    layout.getExportCode()
                            );
                            final long[] currentLayoutID = new long[1];
                            currentLayout = new Layout(
                                    currentUser != null ? currentUser.getUid() : DEFAULT_UID,
                                    layout.getDisplayName(),
                                    layout.getExportCode(),
                                    layout.isCommentRequired(),
                                    layout.isPictureRequired()
                            );
                            Layout finalCurrentLayout1 = currentLayout;
                            final Thread userInsertThread = new Thread(
                                    () -> currentLayoutID[0] = viewModel.insertLayout(
                                            finalCurrentLayout1
                                    )
                            );
                            userInsertThread .start();
                            try {
                                userInsertThread .join();
                            } catch (InterruptedException e) {
                                LOG.error("layout insert interrupted: ", e);
                            }
                            currentLayout.setUid(currentLayoutID[0]);
                        } else {
                            LOG.debug(
                                    "Found existing layout export code {}",
                                    layout.getExportCode()
                            );
                            currentLayout.setUserID(
                                    currentUser != null ? currentUser.getUid() : DEFAULT_UID
                            );
                            currentLayout.setName(layout.getDisplayName());
                            currentLayout.setCommentRequired(layout.isCommentRequired());
                            currentLayout.setPictureRequired(layout.isPictureRequired());
                            Layout finalCurrentLayout = currentLayout;
                            final Thread lcPurgeThread = new Thread(() -> {
                                LOG.debug("Removing existing layout columns on new thread");
                                List<LayoutColumn> layoutColumns = viewModel
                                        .findAllLayoutColumnsByLayout(finalCurrentLayout);
                                for (LayoutColumn oldLayoutColumn : layoutColumns) {
                                    viewModel.deleteLayoutColumn(oldLayoutColumn);
                                }
                            });
                            lcPurgeThread.start();
                            try {
                                lcPurgeThread.join();
                            } catch (InterruptedException e) {
                                LOG.error("importColumnLayout interrupted: ", e);
                                return false;
                            }
                            Layout finalLayout = currentLayout;
                            new Thread(() -> viewModel.updateLayout(finalLayout)).start();
                        }

                        List<JSONLayoutColumn> jsonLayoutColumns = layout.getLayoutColumns();
                        for (JSONLayoutColumn newLayoutColumn : jsonLayoutColumns) {
                            Column foundColumn = currentColumns.stream()
                                    .filter(
                                            column -> column.getExportCode().equalsIgnoreCase(
                                                    newLayoutColumn.getColumnCode()
                                            )
                                    ).findFirst()
                                    .orElse(null);
                            if (foundColumn != null) {
                                Layout finalCurrentLayout2 = currentLayout;
                                new Thread(
                                        () -> viewModel.insertLayoutColumn(
                                                new LayoutColumn(
                                                        finalCurrentLayout2.getUid(),
                                                        foundColumn.getUid(),
                                                        newLayoutColumn.getColumnPosition()
                                                )
                                        )
                                ).start();
                                LOG.debug(
                                        "Column found for {}, adding to layout",
                                        newLayoutColumn.getColumnCode()
                                );
                            } else {
                                LOG.error(
                                        "No column found for {} at position {}, aborting",
                                        newLayoutColumn.getColumnCode(),
                                        newLayoutColumn.getColumnPosition()
                                );
                                return false;
                            }
                        }
                    }
                    LOG.debug("Import successful, saving updated layout");
                    return true;
                } else {
                    LOG.error("No layouts found");
                    return false;
                }
            } else {
                LOG.debug("jsonString does not contain Layouts");
                return false;
            }
        } else {
            LOG.error("jsonString is null");
            return false;
        }
    }

    public boolean importOptions(File uri) {
        LOG.debug("Starting options import");
        final String jsonString = getJsonString(uri);
        final Type type = new TypeToken<Options>() {}.getType();
        if (jsonString != null) {
            if (jsonString.contains("Options")) {
                final Options options;
                try {
                    options = gson.fromJson(jsonString, type);
                } catch (JsonSyntaxException e) {
                    LOG.error("unable to parse jsonString: ", e);
                    return false;
                }
                if (options != null) {
                    options.getOptions().forEach(
                            option -> application.getPreferences().setPref(
                                    option.getKey(),
                                    option.getValue()
                            )
                    );
                } else {
                    LOG.error("options is null");
                    return false;
                }
            } else {
                LOG.debug("jsonString does not contain Options");
                return false;
            }
        } else {
            LOG.error("jsonString is null");
            return false;
        }
        return true;
    }

    public String getJsonString(File file) {
        String jsonString;
        try {
            InputStream is = Files.newInputStream(file.toPath());
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            jsonString = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Exception reading file", e);
            return null;
        }
        return jsonString;
    }
}
