package ph.com.guanzongroup.cas.purchasing.controller;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscReplUtil;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.WebFile;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.token.RequestAccess;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Attachments of one transaction: add, remove, load (download), and upload +
 * save together with the transaction.
 *
 * <p>
 * Nothing here is industry-specific, so every Purchase Order controller (and
 * any other transaction) can hold one:</p>
 * <pre>
 * private final PurchaseOrderAttachments poAttachments = new PurchaseOrderAttachments(poGRider, SOURCE_CODE);
 *
 * //willSave:    poAttachments.prepareForSave(Master().getTransactionNo(), Master().getBranchCode());
 * //saveOthers:  poAttachments.save();
 * </pre>
 *
 */
public class PurchaseOrderAttachments {

    /** WebFile system code for transaction attachments. */
    private static final String WEB_FILE_SYSTEM = "0032";

    private final GRiderCAS poGRider;
    private final String psSourceCode;
    private List<TransactionAttachment> paAttachments = new ArrayList<>();

    public PurchaseOrderAttachments(GRiderCAS foGRider, String fsSourceCode) {
        poGRider = foGRider;
        psSourceCode = fsSourceCode;
    }

    /** Local folder where attachments are kept before upload / after download. */
    protected String getTempPath() {
        return System.getProperty("sys.default.path.temp.attachments");
    }

    /** Access token file used for upload/download. */
    protected String getAccessTokenFile() {
        return System.getProperty("sys.default.access.token");
    }

    // =====================================================================
    // List
    // =====================================================================
    public void reset() {
        paAttachments = new ArrayList<>();
    }

    public TransactionAttachment get(int fnRow) {
        return paAttachments.get(fnRow);
    }

    public int getCount() {
        return paAttachments.size();
    }

    /** Attachments not flagged as removed (inactive). */
    public int getActiveCount() {
        int lnCount = 0;
        for (TransactionAttachment loAttachment : paAttachments) {
            if (!RecordStatus.INACTIVE.equals(loAttachment.getModel().getRecordStatus())) {
                lnCount++;
            }
        }
        return lnCount;
    }

    private TransactionAttachment newAttachment() throws SQLException, GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }

    /** Adds an empty attachment row (every new row is initialized with newRecord()). */
    public JSONObject add() throws SQLException, GuanzonException {
        if (!paAttachments.isEmpty()) {
            String lsLastTransNo = paAttachments.get(paAttachments.size() - 1).getModel().getTransactionNo();
            if (lsLastTransNo == null || lsLastTransNo.isEmpty()) {
                return error("Unable to add transaction attachment.");
            }
        }

        TransactionAttachment loAttachment = newAttachment();
        JSONObject loJSON = loAttachment.newRecord();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }
        paAttachments.add(loAttachment);
        return success();
    }

    /**
     * Adds a file, or re-activates it when the same file was removed earlier.
     *
     * @return row of the attachment, or -1 when it could not be added
     */
    public int add(String fsFileName, String fsSourceNo) throws SQLException, GuanzonException {
        for (int lnCtr = 0; lnCtr < paAttachments.size(); lnCtr++) {
            if (fsFileName.equals(paAttachments.get(lnCtr).getModel().getFileName())
                    && RecordStatus.INACTIVE.equals(paAttachments.get(lnCtr).getModel().getRecordStatus())) {
                paAttachments.get(lnCtr).getModel().setRecordStatus(RecordStatus.ACTIVE);
                return lnCtr;
            }
        }

        if (!isSuccess(add())) {
            return -1;
        }
        int lnRow = paAttachments.size() - 1;
        paAttachments.get(lnRow).getModel().setFileName(fsFileName);
        paAttachments.get(lnRow).getModel().setSourceNo(fsSourceNo);
        paAttachments.get(lnRow).getModel().setRecordStatus(RecordStatus.ACTIVE);
        return lnRow;
    }

    /** New rows are dropped; saved rows are flagged inactive. */
    public JSONObject remove(int fnRow) {
        if (paAttachments.isEmpty()) {
            return error("No transaction attachment to be removed.");
        }
        if (fnRow < 0 || fnRow >= paAttachments.size()) {
            return error("Invalid attachment row.");
        }

        if (paAttachments.get(fnRow).getEditMode() == EditMode.ADDNEW) {
            paAttachments.remove(fnRow);
        } else {
            paAttachments.get(fnRow).getModel().setRecordStatus(RecordStatus.INACTIVE);
        }
        return success();
    }

    /** Copies a file the user picked into the temp attachment folder. */
    public JSONObject copyToTemp(String fsPath) {
        try {
            Path loSource = Paths.get(fsPath);
            Path loTargetDir = Paths.get(getTempPath());
            if (!Files.exists(loTargetDir)) {
                Files.createDirectories(loTargetDir);
            }
            Files.copy(loSource, loTargetDir.resolve(loSource.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            return success();
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to copy " + fsPath, ex);
            return error("Unable to copy file: " + ex.getMessage());
        }
    }

    // =====================================================================
    // Load
    // =====================================================================
    /**
     * Opens the transaction's attachments and downloads each file to the temp
     * folder. A failed download is reported in the result but does not stop
     * the other files from loading.
     *
     * @param fbForUpdate open the attachment records for editing
     */
    public JSONObject load(String fsSourceNo, boolean fbForUpdate) throws SQLException, GuanzonException {
        paAttachments = new ArrayList<>();
        List<String> laFailed = new ArrayList<>();

        List loList = newAttachment().getAttachments(psSourceCode, fsSourceNo);
        for (Object loId : loList) {
            TransactionAttachment loAttachment = newAttachment();
            JSONObject loJSON = loAttachment.openRecord((String) loId);
            if (isSuccess(loJSON) && fbForUpdate) {
                loAttachment.updateRecord();
            }
            paAttachments.add(loAttachment);

            if (!download(loAttachment)) {
                laFailed.add(loAttachment.getModel().getFileName());
            }
        }

        if (!laFailed.isEmpty()) {
            return error("Unable to download: " + String.join(", ", laFailed));
        }
        return success();
    }

    private boolean download(TransactionAttachment foAttachment) {
        try {
            JSONObject loJSON = WebFile.DownloadFile(WebFile.getAccessToken(getAccessTokenFile()),
                    WEB_FILE_SYSTEM, "",
                    foAttachment.getModel().getFileName(),
                    psSourceCode,
                    foAttachment.getModel().getSourceNo(),
                    "");
            if (!isSuccess(loJSON)) {
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Download failed: {0}", loJSON.toJSONString());
                return false;
            }

            JSONObject loPayload = (JSONObject) loJSON.get("payload");
            return WebFile.Base64ToFile((String) loPayload.get("data"),
                    (String) loPayload.get("hash"),
                    getTempPath() + "/",
                    (String) loPayload.get("filename"));
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Download failed", ex);
            return false;
        }
    }

    // =====================================================================
    // Save (called from the controller's willSave / saveOthers)
    // =====================================================================
    /**
     * Before saving: links each attachment to the transaction, renames new
     * files whose name is already used, and uploads files not yet sent.
     * Stops at the first upload error.
     */
    public JSONObject prepareForSave(String fsSourceNo, String fsBranchCode) throws SQLException, GuanzonException {
        for (int lnCtr = 0; lnCtr < paAttachments.size(); lnCtr++) {
            TransactionAttachment loAttachment = paAttachments.get(lnCtr);
            loAttachment.getModel().setSourceNo(fsSourceNo);
            loAttachment.getModel().setSourceCode(psSourceCode);
            loAttachment.getModel().setBranchCode(fsBranchCode);
            loAttachment.getModel().setImagePath(getTempPath());

            String lsOriginalFileName = loAttachment.getModel().getFileName();

            //new file whose name is already used: copy it under the next free name
            if (EditMode.ADDNEW == loAttachment.getModel().getEditMode()) {
                String lsNewFileName = nextFreeFileName(lsOriginalFileName);
                if (!lsNewFileName.equals(lsOriginalFileName)) {
                    JSONObject loJSON = copyAs(lsOriginalFileName, lsNewFileName);
                    if (!isSuccess(loJSON)) {
                        return loJSON;
                    }
                    loAttachment.getModel().setFileName(lsNewFileName);
                }
            }

            //upload when not yet sent
            if ("0".equals(loAttachment.getModel().getSendStatus())) {
                JSONObject loJSON = upload(lnCtr, lsOriginalFileName);
                if (!isSuccess(loJSON)) {
                    return loJSON;
                }
            }
        }
        return success();
    }

    /** Saves new and edited attachment records (inside the transaction's save). */
    public JSONObject save() throws SQLException, GuanzonException, CloneNotSupportedException {
        for (TransactionAttachment loAttachment : paAttachments) {
            if (loAttachment.getEditMode() == EditMode.ADDNEW || loAttachment.getEditMode() == EditMode.UPDATE) {
                loAttachment.getModel().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                loAttachment.getModel().setModifiedDate(poGRider.getServerDate());
                loAttachment.setWithParentClass(true);
                JSONObject loJSON = loAttachment.saveRecord();
                if ("error".equals((String) loJSON.get("result"))) {
                    return loJSON;
                }
            }
        }
        return success();
    }

    /**
     * Uploads one attachment. Uses the new file name when that file exists,
     * otherwise the original one.
     */
    public JSONObject upload(int fnRow, String fsOriginalFileName) {
        TransactionAttachment loAttachment = paAttachments.get(fnRow);
        String lsPath = loAttachment.getModel().getImagePath();
        String lsFile = loAttachment.getModel().getFileName();

        File loFile = new File(lsPath + "/" + lsFile);
        if (!loFile.exists()) {
            lsFile = fsOriginalFileName;
            loFile = new File(lsPath + "/" + lsFile);
            if (!loFile.exists()) {
                return error("Cannot locate file in " + lsPath + "/" + lsFile
                        + ".\nContact system administrator for assistance.");
            }
        }

        try {
            String lsHash = loAttachment.getModel().getMD5Hash();
            if (lsHash == null || lsHash.isEmpty()) {
                lsHash = MiscReplUtil.md5Hash(lsPath + "/" + lsFile);
            }

            String lsToken = getAccessToken(getAccessTokenFile());
            if (lsToken == null) {
                return error("Unable to get an access token for uploading.\nContact system administrator for assistance.");
            }

            JSONObject loResult = WebFile.UploadFile(lsToken,
                    WEB_FILE_SYSTEM, "",
                    loAttachment.getModel().getFileName(),
                    poGRider.getBranchCode(),
                    lsHash,
                    encodeFileToBase64(loFile),
                    loAttachment.getModel().getSourceCode(),
                    loAttachment.getModel().getSourceNo(),
                    "");

            if ("error".equalsIgnoreCase((String) loResult.get("result"))) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Upload failed: {0}", loResult.toJSONString());
                return error("System error while uploading file " + loAttachment.getModel().getFileName()
                        + ".\nContact system administrator for assistance.");
            }

            loAttachment.getModel().setMD5Hash(lsHash);
            loAttachment.getModel().setSendStatus("1");
            return success();
        } catch (Exception ex) {
            //previously swallowed: the PO saved with the file marked unsent
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return error("Unable to upload file " + loAttachment.getModel().getFileName() + ": " + MiscUtil.getException(ex));
        }
    }

    // =====================================================================
    // File names
    // =====================================================================
    /** Whether the file name is already used by a saved attachment. */
    public boolean isFileNameUsed(String fsFileName) throws SQLException, GuanzonException {
        return getUsedFileNames(fsFileName).contains(fsFileName);
    }

    /**
     * The file name, or "name_1.ext", "name_2.ext", ... when it is already
     * used. One query instead of one per attempt.
     */
    public String nextFreeFileName(String fsFileName) throws SQLException, GuanzonException {
        int lnDot = fsFileName.lastIndexOf('.');
        String lsBase = lnDot == -1 ? fsFileName : fsFileName.substring(0, lnDot);
        String lsExt = lnDot == -1 ? "" : fsFileName.substring(lnDot);

        Set<String> loUsed = getUsedFileNames(lsBase);
        String lsName = fsFileName;
        for (int lnCopy = 1; loUsed.contains(lsName); lnCopy++) {
            lsName = lsBase + "_" + lnCopy + lsExt;
        }
        return lsName;
    }

    private Set<String> getUsedFileNames(String fsPrefix) throws SQLException, GuanzonException {
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(newAttachment().getModel()),
                " sFileName LIKE " + SQLUtil.toSQL(fsPrefix + "%"));
        Set<String> loNames = new HashSet<>();
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (loRS == null) {
            return loNames;
        }
        try {
            while (loRS.next()) {
                String lsName = loRS.getString("sFileName");
                if (lsName != null && !lsName.isEmpty()) {
                    loNames.add(lsName);
                }
            }
        } finally {
            MiscUtil.close(loRS);
        }
        return loNames;
    }

    private JSONObject copyAs(String fsFileName, String fsNewFileName) {
        Path loSource = Paths.get(getTempPath(), fsFileName);
        Path loTarget = Paths.get(getTempPath()).resolve(fsNewFileName);
        try {
            Files.copy(loSource, loTarget, StandardCopyOption.REPLACE_EXISTING);
            return success();
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to copy " + loSource, ex);
            return error("Unable to copy attachment " + fsFileName + " as " + fsNewFileName + ".");
        }
    }

    private static String encodeFileToBase64(File foFile) throws IOException {
        return new String(Base64.encodeBase64(Files.readAllBytes(foFile.toPath())), StandardCharsets.UTF_8);
    }

    // =====================================================================
    // Access token (shared by all instances; refreshed when 25 minutes old)
    // =====================================================================
    private static final Object TOKEN_LOCK = new Object();
    private static JSONObject poToken = null;

    /** Access key from the token file, refreshing it when expired. Null when unavailable. */
    private static String getAccessToken(String fsTokenFile) {
        synchronized (TOKEN_LOCK) {
            try {
                if (poToken == null) {
                    poToken = readToken(fsTokenFile);
                }

                Calendar loLimit = Calendar.getInstance();
                loLimit.add(Calendar.MINUTE, -25);
                Calendar loCreated = Calendar.getInstance();
                loCreated.setTime(SQLUtil.toDate((String) poToken.get("created"), SQLUtil.FORMAT_TIMESTAMP));

                //request a new access token when the current one is too old
                if (loLimit.after(loCreated)) {
                    RequestAccess.main(new String[]{(String) poToken.get("parent"), fsTokenFile});
                    poToken = readToken(fsTokenFile);
                }
                return (String) poToken.get("access_key");
            } catch (IOException | ParseException ex) {
                Logger.getLogger(PurchaseOrderAttachments.class.getName()).log(Level.SEVERE, "Unable to read access token", ex);
                poToken = null;
                return null;
            }
        }
    }

    private static JSONObject readToken(String fsTokenFile) throws IOException, ParseException {
        try (Reader loReader = new FileReader(fsTokenFile)) {
            return (JSONObject) new JSONParser().parse(loReader);
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    private static JSONObject success() {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }

    private static JSONObject error(String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    private static boolean isSuccess(JSONObject foJSON) {
        return foJSON != null && "success".equals((String) foJSON.get("result"));
    }
}
