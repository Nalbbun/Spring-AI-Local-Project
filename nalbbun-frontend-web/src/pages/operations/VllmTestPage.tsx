import { useEffect, useMemo, useState } from 'react';
import { settingsApi } from '../../services/settingsApi';
import { notifyGlobal } from '../../lib/uiFeedback';

type Mode = 'LLM' | 'SLLM';

const splitLines = (value: string) =>
  (value || '')
    .split(/+/)
    .map((item) => item.trim())
    .filter(Boolean);

const pretty = (value: unknown) => JSON.stringify(value, null, 2);

export function VllmTestPage() {
  const [mode, setMode] = useState<Mode>('LLM');
  const [model, setModel] = useState('');
  const [systemPrompt, setSystemPrompt] = useState('당신은 유능한 AI 조수입니다.');
  const [userPrompt, setUserPrompt] = useState('안녕하세요. vLLM 테스트입니다.');
  const [texts, setTexts] = useState('안녕하세요, 인공지능 클러스터 테스트입니다./n임베딩 벡터를 생성합니다.');
  const [rerankQuery, setRerankQuery] = useState('임플란트 혜택');
  const [rerankDocuments, setRerankDocuments] = useState('본인부담금 30% 지원 안내/n일반 스케일링 비용 안내/n65세 이상 틀니 혜택');
  const [providerStatus, setProviderStatus] = useState<any>(null);
  const [result, setResult] = useState<any>(null);
  const [isBusy, setIsBusy] = useState(false);

  const textItems = useMemo(() => splitLines(texts), [texts]);
  const rerankItems = useMemo(() => splitLines(rerankDocuments), [rerankDocuments]);

  const requestPreview = useMemo(() => ({
    chat: {
      mode,
      model: model || undefined,
      systemPrompt,
      userPrompt,
    },
    embedding: {
      texts: textItems,
    },
    rerank: {
      query: rerankQuery,
      documents: rerankItems,
      topK: 3,
    },
  }), [mode, model, systemPrompt, userPrompt, textItems, rerankQuery, rerankItems]);

  const refreshStatus = async () => {
    const status = await settingsApi.getVllmStatus();
    setProviderStatus(status);
    return status;
  };

  useEffect(() => {
    refreshStatus().catch(() => undefined);
  }, []);

  const runBusy = async (message: string, action: () => Promise<any>) => {
    if (isBusy) return;
    setIsBusy(true);
    try {
      const response = await action();
      setResult(response);
      notifyGlobal(message, 'success');
      await refreshStatus().catch(() => undefined);
    } catch (error) {
      notifyGlobal(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      setIsBusy(false);
    }
  };

  return (
    <div className="page-stack">
      <section className="card-panel">
        <div className="section-heading">
          <h2>vLLM 자동 설정 / 상태 확인</h2>
          <p>/api/info 기준 자동 설정 및 현재 연결 상태 확인</p>
        </div>
        <div className="button-row">
          <button type="button" onClick={() => runBusy('vLLM 자동 설정이 완료되었습니다.', () => settingsApi.syncVllmFromInfo())} disabled={isBusy}>
            /api/info 기준 자동 설정
          </button>
          <button type="button" className="secondary" onClick={() => runBusy('vLLM 상태를 새로고침했습니다.', refreshStatus)} disabled={isBusy}>
            상태 새로고침
          </button>
        </div>
        <div className="list-grid top-gap">
          <div className="list-item-row"><span>Status</span><span>{providerStatus?.status || '-'}</span></div>
          <div className="list-item-row"><span>Message</span><span>{providerStatus?.message || '-'}</span></div>
          <div className="list-item-row"><span>Base URL</span><span>{providerStatus?.baseUrl || '-'}</span></div>
          <div className="list-item-row"><span>Health URL</span><span>{providerStatus?.resolvedHealthUrl || '-'}</span></div>
          <div className="list-item-row"><span>Models URL</span><span>{providerStatus?.resolvedModelsUrl || '-'}</span></div>
          <div className="list-item-row"><span>LLM URL</span><span>{providerStatus?.resolvedLlmUrl || '-'}</span></div>
          <div className="list-item-row"><span>SLLM URL</span><span>{providerStatus?.resolvedSllmUrl || '-'}</span></div>
          <div className="list-item-row"><span>Embedding URL</span><span>{providerStatus?.resolvedEmbeddingUrl || '-'}</span></div>
          <div className="list-item-row"><span>Rerank URL</span><span>{providerStatus?.resolvedRerankUrl || '-'}</span></div>
          <div className="list-item-row"><span>Search Alias</span><span>{providerStatus?.searchModel || '-'}</span></div>
          <div className="list-item-row"><span>Answer Alias</span><span>{providerStatus?.answerModel || '-'}</span></div>
          <div className="list-item-row"><span>Embedding Alias</span><span>{providerStatus?.embeddingModel || '-'}</span></div>
          <div className="list-item-row"><span>Rerank Alias</span><span>{providerStatus?.rerankModel || '-'}</span></div>
        </div>
      </section>

      <section className="card-panel">
        <div className="section-heading"><h2>채팅 테스트</h2></div>
        <div className="form-grid two-columns">
          <label className="field-label">모드
            <select value={mode} onChange={(e) => setMode(e.target.value as Mode)}>
              <option value="LLM">LLM</option>
              <option value="SLLM">SLLM</option>
            </select>
          </label>
          <label className="field-label">모델 alias(선택)
            <input value={model} onChange={(e) => setModel(e.target.value)} placeholder="비우면 기본 alias 사용" />
          </label>
          <label className="field-label full-span">System Prompt
            <textarea rows={3} value={systemPrompt} onChange={(e) => setSystemPrompt(e.target.value)} />
          </label>
          <label className="field-label full-span">User Prompt
            <textarea rows={4} value={userPrompt} onChange={(e) => setUserPrompt(e.target.value)} />
          </label>
        </div>
        <div className="button-row">
          <button type="button" onClick={() => runBusy('vLLM 채팅 테스트가 완료되었습니다.', () => settingsApi.testVllmChat({ mode, model, systemPrompt, userPrompt }))} disabled={isBusy}>
            채팅 테스트 실행
          </button>
        </div>
      </section>

      <section className="card-panel">
        <div className="section-heading"><h2>임베딩 / 리랭크 테스트</h2></div>
        <div className="form-grid two-columns">
          <label className="field-label">임베딩 텍스트(줄바꿈 구분)
            <textarea rows={5} value={texts} onChange={(e) => setTexts(e.target.value)} />
          </label>
          <div className="stack-gap">
            <label className="field-label">리랭크 Query
              <input value={rerankQuery} onChange={(e) => setRerankQuery(e.target.value)} />
            </label>
            <label className="field-label">리랭크 문서(줄바꿈 구분)
              <textarea rows={5} value={rerankDocuments} onChange={(e) => setRerankDocuments(e.target.value)} />
            </label>
          </div>
        </div>
        <div className="button-row">
          <button type="button" onClick={() => runBusy('vLLM 임베딩 테스트가 완료되었습니다.', () => settingsApi.testVllmEmbedding({ texts: textItems }))} disabled={isBusy}>
            임베딩 테스트
          </button>
          <button type="button" className="secondary" onClick={() => runBusy('vLLM 리랭크 테스트가 완료되었습니다.', () => settingsApi.testVllmRerank({ query: rerankQuery, documents: rerankItems, topK: 3 }))} disabled={isBusy}>
            리랭크 테스트
          </button>
        </div>
      </section>

      <section className="card-panel">
        <div className="section-heading"><h2>요청 미리보기</h2></div>
        <pre className="code-panel">{pretty(requestPreview)}</pre>
      </section>

      <section className="card-panel">
        <div className="section-heading"><h2>응답 결과</h2></div>
        <pre className="code-panel">{result ? pretty(result) : '결과 없음'}</pre>
      </section>
    </div>
  );
}
