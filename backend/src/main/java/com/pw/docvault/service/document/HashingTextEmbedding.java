package com.pw.docvault.service.document;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class HashingTextEmbedding {

    static final int DIMENSIONS = 384;

    private HashingTextEmbedding() {
    }

    static float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^\\p{Alnum}]+");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            int hash = fnv1a(token);
            int index = Math.floorMod(hash, DIMENSIONS);
            vector[index] += ((hash & 1) == 0) ? 1.0f : -1.0f;
        }

        normalize(vector);
        return vector;
    }

    private static int fnv1a(String value) {
        int hash = 0x811c9dc5;
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= b & 0xff;
            hash *= 0x01000193;
        }
        return hash;
    }

    private static void normalize(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) {
            return;
        }

        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }
}
