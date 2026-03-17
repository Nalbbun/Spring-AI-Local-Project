package ai.local.nalbbun.category.general.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

/**
 * RuleBasedGeneralParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: rule based general parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class RuleBasedGeneralParser implements CategoryParsingStrategy<GeneralContext> {

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return GeneralContext 타입의 처리 결과
     */
    @Override
    public GeneralContext parse(ConversationState state, GeneralContext context) {
        context.setIntent("general_qa");
        return context;
    }

    /**
     * mode 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    @Override
    public String mode() {
        return "RULE";
    }
}