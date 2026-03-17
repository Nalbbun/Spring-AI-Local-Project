package ai.local.nalbbun.rag.reader;

import org.springframework.core.io.ByteArrayResource;

/**
 * NamedByteArrayResource는 RAG 관련 처리와 관리 기능을 담당하는 컴포넌트이다.
 * <p>주요 기능: named byte array resource 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public class NamedByteArrayResource extends ByteArrayResource {

    /** filename 값을 보관한다. */
    private final String filename;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param byteArray byteArray 값
     * @param filename filename 값
     */
    public NamedByteArrayResource(byte[] byteArray, String filename) {
        super(byteArray);
        this.filename = filename;
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    @Override
    public String getFilename() {
        return filename;
    }
}
