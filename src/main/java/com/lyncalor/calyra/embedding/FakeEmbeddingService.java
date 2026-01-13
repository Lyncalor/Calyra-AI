package com.lyncalor.calyra.embedding;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public class FakeEmbeddingService implements EmbeddingService {

    private final int dimension;

    public FakeEmbeddingService(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public List<Float> embed(String text) {
        String input = text == null ? "" : text;
        byte[] hash = sha256(input);
        long seed = ByteBuffer.wrap(hash).getLong();
        SplittableRandom random = new SplittableRandom(seed);
        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            float value = (float) (random.nextDouble() * 2.0 - 1.0);
            vector.add(value);
        }
        return vector;
    }

    private byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
