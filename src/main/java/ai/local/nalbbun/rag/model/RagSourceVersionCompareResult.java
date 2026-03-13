package ai.local.nalbbun.rag.model;

import java.util.List;

import lombok.Builder;

@Builder
public record RagSourceVersionCompareResult(
        String category,
        String source,
        String leftVersion,
        String rightVersion,
        RagSourceManifest left,
        RagSourceManifest right,
        int leftVectorRows,
        int rightVectorRows,
        String query,
        List<RagRetrievedDocument> leftHits,
        List<RagRetrievedDocument> rightHits,
        String summary
) {
}
