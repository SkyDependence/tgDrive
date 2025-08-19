package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.dto.CheckChunkResponse;
import com.skydevs.tgdrive.dto.ChunkUploadRequest;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ChunkStorageService;
import com.skydevs.tgdrive.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final ChunkStorageService chunkStorageService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<CheckChunkResponse> checkChunks(
            @RequestParam("identifier") String identifier) {
        
        // This is a placeholder for now. We will implement the file existence check later.
        // boolean fileExists = fileStorageService.isFileExists(identifier);
        // if (fileExists) {
        //     return ResponseEntity.ok(new CheckChunkResponse(true, null));
        // }

        Set<Integer> uploadedChunks = chunkStorageService.checkUploadedChunks(identifier);
        return ResponseEntity.ok(new CheckChunkResponse(false, uploadedChunks));
    }

    @PostMapping
    public ResponseEntity<Void> uploadChunk(
            @RequestParam("identifier") String identifier,
            @RequestParam("chunkNumber") Integer chunkNumber,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("totalSize") Long totalSize,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file) {

        ChunkUploadRequest request = new ChunkUploadRequest();
        request.setIdentifier(identifier);
        request.setChunkNumber(chunkNumber);
        request.setTotalChunks(totalChunks);
        request.setTotalSize(totalSize);
        request.setFilename(filename);
        request.setFile(file);

        chunkStorageService.saveChunk(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/merge")
    public CompletableFuture<Result<UploadFile>> mergeChunks(
            @RequestParam("identifier") String identifier,
            @RequestParam("filename") String filename,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("totalSize") long totalSize,
            HttpServletRequest request) {
        
        // Assuming userId is retrieved from the request/session
        Long userId = (Long) request.getAttribute("userId");

        return CompletableFuture.supplyAsync(() -> 
            fileStorageService.mergeFile(identifier, filename, totalChunks, totalSize, request, userId)
        ).thenApply(Result::success);
    }
}