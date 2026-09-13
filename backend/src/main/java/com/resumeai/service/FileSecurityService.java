package com.resumeai.service;

import com.resumeai.exception.InvalidFileException;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileSecurityService {

    private final Tika tika = new Tika();

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf",
            "docx",
            "txt"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final long maxFileSizeBytes;

    public FileSecurityService(
            @Value("${app.upload.max-file-size-mb}") long maxFileSizeMb
    ) {
        this.maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
    }

    public record ValidatedFile(
            byte[] bytes,
            String detectedMimeType,
            String extension,
            String sanitizedDisplayName,
            String contentHashSha256
    ) {
    }

    public ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("The uploaded file is empty.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidFileException(
                    "File exceeds the maximum allowed size of "
                            + (maxFileSizeBytes / (1024 * 1024))
                            + "MB."
            );
        }

        String extension = extractExtension(file.getOriginalFilename());

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                    "Unsupported file type. Please upload a PDF, DOCX, or TXT file."
            );
        }

        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidFileException(
                    "Could not read the uploaded file."
            );
        }

        String detectedMimeType = detectSupportedMimeType(bytes, extension);

        if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
            throw new InvalidFileException(
                    "The file's content does not match a supported format (PDF, DOCX, or TXT). " +
                    "It may be corrupted, mislabeled, or a disguised file type."
            );
        }

        assertExtensionMatchesDetectedType(
                extension,
                detectedMimeType
        );

        if ("docx".equals(extension)) {
            guardAgainstZipBomb(bytes);
        }

        String sanitizedName = sanitizeFilenameForDisplay(
                file.getOriginalFilename()
        );

        String hash = sha256(bytes);

        return new ValidatedFile(
                bytes,
                detectedMimeType,
                extension,
                sanitizedName,
                hash
        );
    }

    private String detectSupportedMimeType(
            byte[] bytes,
            String extension
    ) {
        try {
            if (isPdf(bytes)) {
                return "application/pdf";
            }

            if (isDocx(bytes)) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }

            String detected = tika.detect(bytes);

            if ("txt".equals(extension) && detected.startsWith("text/")) {
                return "text/plain";
            }

            return detected;
        } catch (Exception e) {
            throw new InvalidFileException(
                    "Could not determine the file type from its contents."
            );
        }
    }

    private boolean isPdf(byte[] bytes) {
        return bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-';
    }

    private boolean isDocx(byte[] bytes) {
        if (bytes.length < 4
                || bytes[0] != 'P'
                || bytes[1] != 'K') {
            return false;
        }

        boolean contentTypesFound = false;
        boolean documentXmlFound = false;

        try (ZipInputStream zis =
                     new ZipInputStream(
                             new ByteArrayInputStream(bytes)
                     )) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if ("[Content_Types].xml".equals(name)) {
                    contentTypesFound = true;
                }

                if ("word/document.xml".equals(name)) {
                    documentXmlFound = true;
                }

                if (contentTypesFound && documentXmlFound) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            return false;
        }

        return false;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        String name = originalFilename.trim();

        int dot = name.lastIndexOf('.');

        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }

        return name
                .substring(dot + 1)
                .toLowerCase(Locale.ROOT);
    }

    private void assertExtensionMatchesDetectedType(
            String extension,
            String detectedMimeType
    ) {
        boolean matches = switch (extension) {
            case "pdf" ->
                    detectedMimeType.equals("application/pdf");

            case "docx" ->
                    detectedMimeType.equals(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    );

            case "txt" ->
                    detectedMimeType.startsWith("text/");

            default ->
                    false;
        };

        if (!matches) {
            throw new InvalidFileException(
                    "The file extension does not match its actual content type. " +
                    "This file was rejected for your safety."
            );
        }
    }

    private void guardAgainstZipBomb(byte[] bytes) {
        final int maxEntries = 2000;
        final long maxTotalUncompressedBytes =
                200L * 1024 * 1024;
        final long maxSingleEntryUncompressedBytes =
                100L * 1024 * 1024;
        final int maxCompressionRatio = 300;

        final byte[] buffer = new byte[8192];

        int entryCount = 0;
        long totalUncompressed = 0;

        try (
                ZipInputStream zis =
                        new ZipInputStream(
                                new ByteArrayInputStream(bytes)
                        )
        ) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;

                if (entryCount > maxEntries) {
                    throw new InvalidFileException(
                            "This DOCX file contains an unusually large number " +
                            "of internal parts and was rejected for your safety."
                    );
                }

                long entryUncompressed = 0;

                long compressedSize =
                        entry.getCompressedSize();

                long compressedSizeAtStart =
                        compressedSize > 0
                                ? compressedSize
                                : 1;

                int read;

                while ((read = zis.read(buffer)) != -1) {
                    entryUncompressed += read;
                    totalUncompressed += read;

                    if (entryUncompressed >
                            maxSingleEntryUncompressedBytes
                            || totalUncompressed >
                            maxTotalUncompressedBytes) {

                        throw new InvalidFileException(
                                "This DOCX file expands to an unusually large " +
                                "size and was rejected for your safety."
                        );
                    }

                    if (entryUncompressed > 1_000_000
                            && entryUncompressed /
                            compressedSizeAtStart >
                            maxCompressionRatio) {

                        throw new InvalidFileException(
                                "This DOCX file has a suspicious compression ratio " +
                                "and was rejected for your safety."
                        );
                    }
                }

                zis.closeEntry();
            }
        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidFileException(
                    "This DOCX file could not be safely inspected and was rejected."
            );
        }
    }

    private String sanitizeFilenameForDisplay(
            String originalFilename
    ) {
        if (originalFilename == null) {
            return "resume";
        }

        String base = originalFilename
                .replaceAll("[/\\\\]", "");

        String cleaned = base
                .replaceAll(
                        "[^a-zA-Z0-9._ -]",
                        "_"
                );

        if (cleaned.isBlank()) {
            return "resume";
        }

        return cleaned.substring(
                0,
                Math.min(cleaned.length(), 255)
        );
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(bytes);

            StringBuilder sb = new StringBuilder();

            for (byte b : hash) {
                sb.append(
                        String.format("%02x", b)
                );
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 not available",
                    e
            );
        }
    }

    public List<String> allowedExtensions() {
        return ALLOWED_EXTENSIONS.stream().toList();
    }
}