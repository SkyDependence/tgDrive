package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.dto.ChunkUploadRequest;
import com.skydevs.tgdrive.service.ChunkStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ChunkStorageServiceImpl implements ChunkStorageService {

    private final Path chunkDir;

    public ChunkStorageServiceImpl() {
        String tempDir = System.getProperty("java.io.tmpdir");
        this.chunkDir = Paths.get(tempDir, "tgdrive-chunks");
        if (Files.notExists(chunkDir)) {
            try {
                Files.createDirectories(chunkDir);
            } catch (IOException e) {
                log.error("Failed to create chunk directory: {}", chunkDir, e);
                throw new RuntimeException("Failed to create chunk directory", e);
            }
        }
    }

    @Override
    public void saveChunk(ChunkUploadRequest request) {
        Path chunkPath = getChunkPath(request.getIdentifier(), request.getChunkNumber());
        try {
            Files.createDirectories(chunkPath.getParent());
            Files.write(chunkPath, request.getFile().getBytes());
        } catch (IOException e) {
            log.error("Failed to save chunk: {}", chunkPath, e);
            throw new RuntimeException("Failed to save chunk", e);
        }
    }

    @Override
    public Set<Integer> checkUploadedChunks(String identifier) {
        Path identifierDir = chunkDir.resolve(identifier);
        if (Files.notExists(identifierDir)) {
            return new HashSet<>();
        }
        try (Stream<Path> stream = Files.list(identifierDir)) {
            return stream
                    .map(path -> Integer.parseInt(path.getFileName().toString()))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("Failed to check uploaded chunks for identifier: {}", identifier, e);
            return new HashSet<>();
        }
    }

    @Override
    public InputStream mergeChunks(String identifier, String filename, int totalChunks) {
        Path identifierDir = chunkDir.resolve(identifier);
        Path mergedFilePath = identifierDir.resolve(filename);

        try {
            // Create the merged file
            Files.deleteIfExists(mergedFilePath);
            Files.createFile(mergedFilePath);

            // Append chunks in order
            for (int i = 1; i <= totalChunks; i++) {
                Path chunkPath = getChunkPath(identifier, i);
                if (Files.exists(chunkPath)) {
                    byte[] chunkBytes = Files.readAllBytes(chunkPath);
                    Files.write(mergedFilePath, chunkBytes, StandardOpenOption.APPEND);
                } else {
                    throw new IOException("Chunk " + i + " is missing for identifier " + identifier);
                }
            }
            return new BufferedInputStream(Files.newInputStream(mergedFilePath));
        } catch (IOException e) {
            log.error("Failed to merge chunks for identifier: {}", identifier, e);
            throw new RuntimeException("Failed to merge chunks", e);
        }
    }

    @Override
    public void cleanupChunks(String identifier) {
        Path identifierDir = chunkDir.resolve(identifier);
        if (Files.exists(identifierDir)) {
            try (Stream<Path> walk = Files.walk(identifierDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Failed to delete path: {}", path, e);
                            }
                        });
            } catch (IOException e) {
                log.error("Failed to cleanup chunks for identifier: {}", identifier, e);
            }
        }
    }

    private Path getChunkPath(String identifier, int chunkNumber) {
        return chunkDir.resolve(identifier).resolve(String.valueOf(chunkNumber));
    }
}
