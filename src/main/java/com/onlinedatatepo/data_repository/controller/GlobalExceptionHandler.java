package com.onlinedatatepo.data_repository.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.apache.catalina.connector.ClientAbortException;

/**
 * Global exception handler for upload and multipart errors.
 * Provides user-friendly error messages instead of whitelabel errors.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle file size exceeded errors (413 Content Too Large).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleFileSizeExceeded(MaxUploadSizeExceededException ex,
                                         RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error",
                "Upload failed: File or total request size exceeds the 300MB limit. Please check your files and try again.");
        return "redirect:/upload/start";
    }

    /**
     * Handle Tomcat SizeLimitExceededException (same as above but from different source).
     */
    @ExceptionHandler(org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException.class)
    public String handleTomcatSizeExceeded(org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException ex,
                                            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error",
                "Upload failed: File or total request size exceeds the 300MB limit. Please check your files and try again.");
        return "redirect:/upload/start";
    }

    /**
     * Handle client abort during upload (connection reset by client).
     */
    @ExceptionHandler(ClientAbortException.class)
    public String handleClientAbort(ClientAbortException ex,
                                    RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("warning",
                "Upload interrupted: The upload was cancelled or your connection was lost. Please try again.");
        return "redirect:/upload/start";
    }
}
