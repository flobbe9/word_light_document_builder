package de.word_light.document_builder.utils;

import java.io.File;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.info.OsInfo;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.word_light.document_builder.exception.ApiException;
import lombok.extern.log4j.Log4j2;


/**
 * Util class holding static helper methods and global variables.
 * 
 * @since 0.0.5
 */
@Log4j2
public class Utils {

    /**
     * Prepends a '/' to given String if there isn't already one.
     * 
     * @param str String to prepend the slash to
     * @return sring with "/" prepended or just "/" if given string is null. Does not alter given str
     */
    public static String prependSlash(String str) {
        if (str == null || str.equals(""))
            return "/";

        return str.charAt(0) == '/' ? str : "/" + str;
    }

    /** 
     * At least <p>
     * - eight characters, <p>
     * - one uppercase letter, <p>
     * - one lowercase letter,  <p>
     * - one number and <p>
     * - one of given special characters. <p>
     * - maximum 30 characters, 
     */
    public static boolean isPasswordValid(String password) {
        if (password == null)
            throw new ApiException("Failed to validate password. 'password' cannot be null");
        
        String regex = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[,.;_!#$%&’*+/=?`{|}~^-]).{8,30}$";

        return password.matches(regex);
    }

    /**
     * Prepends current date and time to given string. Replace ':' with '-' due to .docx naming conditions.
     * 
     * @param str String to format
     * @return current date and time plus str
     */
    public static String prependDateTime(String str) {
        return LocalDateTime.now().toString().replace(":", "-") + "_" + str;
    }
    
    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);

            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param object to convert to json string
     * @return given object as json string
     */
    public static String objectToJson(Object object) {

        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

        try {
            return objectWriter.writeValueAsString(object);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new ApiException("Failed to convert object to json String.", e);
        }
    }


    /**
     * @param millis time to convert in milli seconds
     * @param timeZone to use for conversion, i.e. {@code "UTC"} or {@code "Europe/Berlin"}. If invalid, system default will be used.
     * @return given time as {@link LocalDateTime} object or null if {@code millis} is invalid
     */
    public static LocalDateTime millisToLocalDateTime(long millis, @Nullable String timeZone) {

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timeZone);

        // case: invalid timeZone
        } catch (DateTimeException | NullPointerException e) {
            zoneId = ZoneId.systemDefault();
        }

        try {
            Instant instant = Instant.ofEpochMilli(millis);
            return LocalDateTime.ofInstant(instant, zoneId);
            
        // case: invalid millis
        } catch (DateTimeException e) {
            return null;
        }
    }

    /**
     * Ci indicates that the app is running inside a pipeline or similar. Expect "CI" variable to be defined
     * either as application.property or inside an .env file different then the main ".env".
     * 
     * @return {@code true} or {@code false} (default)
     */
    public static boolean isCI() {
        return System.getProperty("CI", "false").equals("true");
    }

    public static boolean isWindowsOs() {
        return StringUtils.containsIgnoreCase(new OsInfo().getName(), "windows");
    }
    
    public static boolean isLinuxOs() {
        return StringUtils.containsIgnoreCase(new OsInfo().getName(), "linux");
    }

    /**
     * Wont throw if given args itself is {@code null}. 
     * 
     * @param args to check
     * @throws IllegalArgumentException
     */
    public static void assertArgsNotNullAndNotBlankOrThrow(Object ...args) throws IllegalArgumentException {
        if (args == null)
            return;

        for (int i = 0; i < args.length; i++) 
            if (assertNullOrBlank(args[i]))
                throw new IllegalArgumentException("Mehtod arg null or blank at index " + i);
    }
    

    /**
     * @param args to check
     * @return {@code true} if at least one arg is {@code null} or blank (will stop iterating), else {@code false}
     */
    public static boolean assertArgsNullOrBlank(Object ...args) throws IllegalArgumentException {
        if (args == null)
            return true;

        for (int i = 0; i < args.length; i++) 
            if (assertNullOrBlank(args[i]))
                return true;

        return false;
    }

    /**
     * @param obj to check
     * @return {@code true} if given {@code obj} is either {@code null} or (if instance of String) {@link #isBlank(String)}, else {@code false}
     */
    public static boolean assertNullOrBlank(Object obj) {
        if (obj == null)
            return true;

        if (obj instanceof String)
            return StringUtils.isBlank((String) obj);

        return false;
    }

    /**
     * Keep calling {@code condition} callback until it is {@code true} or the {@code waitDuration} is reached.
     * 
     * @param condition
     * @param waitDuration in ms
     * @return
     * @throws Exception if condition throws
     * @throws TimeoutException if {@code waitDuration} is reached before condition was {@code true}
     */
    public static boolean awaitOrThrow(@NonNull Callable<Boolean> condition, int waitDuration) throws Exception {
        assertArgsNotNullAndNotBlankOrThrow(condition);

        Date futureDate = new Date((long) System.currentTimeMillis() + waitDuration);

        while (System.currentTimeMillis() < futureDate.getTime()) {
            if (condition.call())
                return true;

            Thread.sleep(10); 
        }
            
        throw new TimeoutException("Timeout of '%sms' exceeded for awaiting condition".formatted(waitDuration));
    }

    /**
     * Create tmp dir if not exists.
     * 
     * @return file of the linux tmp dir
     * @throws IllegalStateException if the LINUX_TMP_DIR is not defined or if the dir could not be created for some reason
     */
    @NonNull
    public static File getLinuxTmpDir() {
        String path = System.getenv("LINUX_TMP_DIR");

        if (StringUtils.isBlank(path))
            throw new IllegalStateException("Failed to get linux tmp dir. 'path' is blank. Make sure environment 'LINUX_TMP_DIR' is defined.");

        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdir();
            if (!created) 
                throw new IllegalStateException("Failed to create linux tmp dir.");
        }

        return dir;
    }
}