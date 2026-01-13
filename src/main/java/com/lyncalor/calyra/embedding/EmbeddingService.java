package com.lyncalor.calyra.embedding;

import java.util.List;

public interface EmbeddingService {

    int dimension();

    List<Float> embed(String text);
}
