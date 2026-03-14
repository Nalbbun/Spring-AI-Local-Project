let source = null;
    let allOllamaModels = [];
    const agentStateMap = {};

    function setConfigStatus(text) {
        document.getElementById("configStatus").textContent = text;
    }

    function setRuntimeStoreStatus(text) {
        document.getElementById("runtimeStoreStatus").textContent = text;
    }

    function renderMemoryStoreInfo(data) {
        const store = data.memoryStore || 'unknown';
        const serviceType = data.memoryServiceType || 'unknown';
        const fallbackPolicy = data.fallbackPolicy || 'unknown';
        const conversationId = data.conversationId || 'unknown';

        document.getElementById('memoryStoreBadge').textContent = `store: ${store}`;
        document.getElementById('memoryServiceBadge').textContent = `service: ${serviceType}`;
        document.getElementById('statusConversationId').textContent = conversationId;
        document.getElementById('statusStoreType').textContent = store;
        document.getElementById('statusFallbackPolicy').textContent = fallbackPolicy;

        setRuntimeStoreStatus(
            `메모리 저장소
store=${store}
service=${serviceType}
fallbackPolicy=${fallbackPolicy}
conversationId=${conversationId}
* 저장소 변경은 프로필/환경변수 변경 후 재시작이 필요합니다.`
        );
    }

    function renderServerSettings(data) {
        const profiles = Array.isArray(data.activeProfiles) && data.activeProfiles.length > 0
            ? data.activeProfiles.join(', ')
            : '-';
        document.getElementById('serverSettingsPanel').textContent =
            `application=${data.applicationName || '-'}
profiles=${profiles}
debugEnabled=${data.debugEnabled}
serverPort=${data.serverPort || '-'}
datasourceUrl=${data.datasourceUrl || '-'}
datasourceUsername=${data.datasourceUsername || '-'}
redis=${data.redisHost || '-'}:${data.redisPort || '-'}
ollamaBaseUrl=${data.ollamaBaseUrl || '-'}
ragVectorStore=${data.ragVectorStore || '-'}
ragRegistryBaseDir=${data.ragRegistryBaseDir || '-'}
llmTimeoutMs=${data.llmTimeoutMs || '-'}
llmRetryAttempts=${data.llmRetryAttempts || '-'}
llmRetryBackoffMs=${data.llmRetryBackoffMs || '-'}
* datasource/redis/port/profile 등은 조회 전용이며 실제 변경은 재시작이 필요합니다.`;
    }

    async function loadDbInfo() {
        try {
            const data = await fetchJson('/debug/api/rag/db-info');
            document.getElementById('statusDb').textContent = data?.jdbc?.connected ? 'connected' : 'offline';
            document.getElementById('dbInfoPanel').textContent = JSON.stringify(data, null, 2);
        } catch (e) {
            document.getElementById('statusDb').textContent = 'error';
            document.getElementById('dbInfoPanel').textContent = 'DB 현황 조회 실패: ' + e.message;
        }
    }

    function logLine(text) {
        const log = document.getElementById("streamLog");
        log.textContent += text + "\n";
        log.scrollTop = log.scrollHeight;
    }

    function resetStreamUi() {
        document.getElementById("streamLog").textContent = "";
        Object.keys(agentStateMap).forEach(key => delete agentStateMap[key]);
        renderAgentChips();
    }

    function renderAgentChips() {
        const container = document.getElementById("agentChips");
        container.innerHTML = "";

        Object.entries(agentStateMap).forEach(([agent, info]) => {
            const chip = document.createElement("span");
            chip.className = `chip ${info.status || ""}`;
            chip.textContent = `${agent} | ${info.status} | ${info.message}`;
            container.appendChild(chip);
        });
    }

    function applyExample() {
        const value = document.getElementById("example").value;
        if (value) {
            document.getElementById("message").value = value;
        }
    }

    function setLoading(active, text = '처리 중입니다...') {
        document.getElementById('loadingText').textContent = text;
        document.getElementById('loadingOverlay').classList.toggle('active', active);
    }

    async function fetchJson(url, options = {}) {
        const res = await fetch(url, options);
        const text = await res.text();
        let json = null;
        try { json = text ? JSON.parse(text) : null; } catch (e) {}

        if (!res.ok) {
            const message = json?.message || text || `HTTP ${res.status}`;
            const hint = json?.hint ? `\n힌트: ${json.hint}` : '';
            throw new Error(message + hint);
        }

        return json;
    }

    async function loadDebugConfig() {
        try {
            const data = await fetchJson('/debug/api/config');

            document.getElementById('resolverMode').value = data.resolverMode || 'HYBRID';
            document.getElementById('generalParserMode').value = data.generalParserMode || 'HYBRID';
            document.getElementById('travelParserMode').value = data.travelParserMode || 'HYBRID';
            document.getElementById('devParserMode').value = data.devParserMode || 'HYBRID';
            document.getElementById('miceParserMode').value = data.miceParserMode || 'HYBRID';
            document.getElementById('fallbackPolicy').value = data.fallbackPolicy || 'BLOCK_OPENAI';
            document.getElementById('llmTimeoutMs').value = data.llmTimeoutMs || 45000;
            document.getElementById('llmRetryAttempts').value = data.llmRetryAttempts || 2;
            document.getElementById('llmRetryBackoffMs').value = data.llmRetryBackoffMs || 800;
            document.getElementById('ragEnabled').value = String(data.ragEnabled ?? true);
            document.getElementById('ragTopK').value = data.ragTopK || 4;
            document.getElementById('ragSimilarityThreshold').value = data.ragSimilarityThreshold ?? 0.72;
            document.getElementById('ragIncludeCitations').value = String(data.ragIncludeCitations ?? true);
            document.getElementById('ragGeneralEnabled').value = String(data.ragGeneralEnabled ?? false);
            document.getElementById('ragDevEnabled').value = String(data.ragDevEnabled ?? true);
            document.getElementById('ragMiceEnabled').value = String(data.ragMiceEnabled ?? true);
            document.getElementById('ragTravelEnabled').value = String(data.ragTravelEnabled ?? false);
            document.getElementById('ragDatasetLocation').value = data.ragDatasetLocation || '';

            setConfigStatus(
                `설정 조회 완료
resolver=${data.resolverMode}
general=${data.generalParserMode}
travel=${data.travelParserMode}
dev=${data.devParserMode}
mice=${data.miceParserMode}
fallbackPolicy=${data.fallbackPolicy}
ragEnabled=${data.ragEnabled}
ragTopK=${data.ragTopK}
similarityThreshold=${data.ragSimilarityThreshold}
conversationId=${data.conversationId}`
            );
            renderMemoryStoreInfo(data);
            renderServerSettings(data);
            await loadDbInfo();
        } catch (e) {
            setConfigStatus('설정 조회 실패: ' + e.message);
        }
    }

    async function saveDebugConfig() {
        const payload = {
            resolverMode: document.getElementById('resolverMode').value,
            generalParserMode: document.getElementById('generalParserMode').value,
            travelParserMode: document.getElementById('travelParserMode').value,
            devParserMode: document.getElementById('devParserMode').value,
            miceParserMode: document.getElementById('miceParserMode').value,
            fallbackPolicy: document.getElementById('fallbackPolicy').value,
            llmTimeoutMs: Number(document.getElementById('llmTimeoutMs').value || 45000),
            llmRetryAttempts: Number(document.getElementById('llmRetryAttempts').value || 2),
            llmRetryBackoffMs: Number(document.getElementById('llmRetryBackoffMs').value || 800),
            ragEnabled: document.getElementById('ragEnabled').value === 'true',
            ragTopK: Number(document.getElementById('ragTopK').value || 4),
            ragSimilarityThreshold: Number(document.getElementById('ragSimilarityThreshold').value || 0.72),
            ragIncludeCitations: document.getElementById('ragIncludeCitations').value === 'true',
            ragGeneralEnabled: document.getElementById('ragGeneralEnabled').value === 'true',
            ragDevEnabled: document.getElementById('ragDevEnabled').value === 'true',
            ragMiceEnabled: document.getElementById('ragMiceEnabled').value === 'true',
            ragTravelEnabled: document.getElementById('ragTravelEnabled').value === 'true',
            ragDatasetLocation: document.getElementById('ragDatasetLocation').value
        };

        try {
            const data = await fetchJson('/debug/api/config', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });

            setConfigStatus(
                `설정 저장 완료
resolver=${data.resolverMode}
general=${data.generalParserMode}
travel=${data.travelParserMode}
dev=${data.devParserMode}
mice=${data.miceParserMode}
fallbackPolicy=${data.fallbackPolicy}
ragEnabled=${data.ragEnabled}
ragTopK=${data.ragTopK}
similarityThreshold=${data.ragSimilarityThreshold}
conversationId=${data.conversationId}`
            );
            renderMemoryStoreInfo(data);
            renderServerSettings(data);
            await loadDbInfo();
        } catch (e) {
            setConfigStatus('설정 저장 실패: ' + e.message);
        }
    }

    async function resetDebugConfig() {
        try {
            await fetchJson('/debug/api/config/reset', {
                method: 'POST'
            });
            await loadDebugConfig();
            setConfigStatus('설정 초기화 완료');
        } catch (e) {
            setConfigStatus('설정 초기화 실패: ' + e.message);
        }
    }

    function startStream() {
        if (source) {
            source.close();
            source = null;
        }

        resetStreamUi();

        const message = document.getElementById("message").value.trim();
        const category = document.getElementById("category").value.trim();

        if (!message) {
            logLine("[error] 메시지를 입력하세요.");
            return;
        }

        let url = `/api/chat/stream?message=${encodeURIComponent(message)}`;
        if (category) {
            url += `&category=${encodeURIComponent(category)}`;
        }

        logLine(`[request-message] ${message}`);
        logLine(`[request-category] ${category || 'AUTO'}`);

        source = new EventSource(url);

        source.addEventListener("agent", (event) => {
            try {
                const data = JSON.parse(event.data);
                agentStateMap[data.agent] = {
                    status: data.status,
                    message: data.message
                };
                renderAgentChips();
                logLine(`[agent] ${data.agent} | ${data.status} | ${data.message}`);
            } catch (e) {
                logLine(`[agent-parse-error] ${event.data}`);
            }
        });

        source.addEventListener("message", (event) => {
            logLine(`[message]\n${event.data}\n`);
        });

        source.addEventListener("complete", async () => {
            logLine(`[complete] 스트림 종료`);
            if (source) {
                source.close();
                source = null;
            }
            await loadLatestRagTraces();
        });

        source.addEventListener("error", async () => {
            logLine(`[error] 스트림 오류 또는 종료`);
            if (source) {
                source.close();
                source = null;
            }
            await loadLatestRagTraces();
        });
    }

    async function loadMemory() {
        try {
            const data = await fetchJson('/debug/api/memory');
            document.getElementById("memoryPanel").textContent = JSON.stringify(data, null, 2);
        } catch (e) {
            document.getElementById("memoryPanel").textContent = '메모리 조회 실패: ' + e.message;
        }
    }

    async function clearMemory() {
        try {
            const res = await fetch('/debug/api/memory/clear', { method: 'POST' });
            const text = await res.text();
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}: ${text}`);
            }
            document.getElementById("memoryPanel").textContent = text;
            logLine(`[memory] ${text}`);
        } catch (e) {
            document.getElementById("memoryPanel").textContent = '메모리 초기화 실패: ' + e.message;
        }
    }


	function currentRagPayload() {
	    return {
	        category: document.getElementById('ragCategory').value,
	        source: (document.getElementById('ragSource').value || '').trim(),
	        version: (document.getElementById('ragVersion').value || '').trim() || 'v1',
	        title: (document.getElementById('ragTitle').value || '').trim()
	    };
	}

	function setUploadStatus(text, type = 'running') {
	    const box = document.getElementById('uploadStatus');
	    box.className = 'status-box ' + type;
	    box.textContent = text;
	}

	function renderUploadRows(items) {
	    const body = document.getElementById('uploadResultTable');
	    if (!items || items.length === 0) {
	        body.innerHTML = '<tr><td colspan="8">아직 결과가 없습니다.</td></tr>';
	        return;
	    }
	    body.innerHTML = items.map(item => `
	        <tr>
	            <td>${item.fileName || '-'}</td>
	            <td>${item.source || '-'}</td>
	            <td>${item.version || '-'}</td>
	            <td>${item.title || '-'}</td>
	            <td>${item.chunkCount ?? 0}</td>
	            <td>${item.traceId ? `<button class="secondary" style="width:auto;padding:6px 10px;" onclick="selectRagTrace('${item.traceId}')">${item.traceId}</button>` : '-'}</td>
	            <td>${item.stored ? 'SUCCESS' : 'FAIL'}</td>
	            <td>${item.message || '-'}</td>
	        </tr>`).join('');
	}

	function applyUploadIdentity(result) {
	    if (!result) return;
	    if (result.source) document.getElementById('ragSource').value = result.source;
	    if (result.version) document.getElementById('ragVersion').value = result.version;
	    if (result.title) document.getElementById('ragTitle').value = result.title;
	    if (result.traceId) document.getElementById('ragTraceId').value = result.traceId;
	}

	async function uploadSingleFile() {
	    const file = document.getElementById('ragFile').files[0];
	    if (!file) {
	        setUploadStatus('단일 파일 업로드 실패\n파일을 먼저 선택하세요.', 'error');
	        return;
	    }
	    const payload = currentRagPayload();
	    const form = new FormData();
	    form.append('category', payload.category);
	    form.append('source', payload.source);
	    form.append('version', payload.version);
	    form.append('title', payload.title);
	    form.append('file', file);
	    try {
	        setLoading(true, '단일 파일 업로드 중...');
	        setUploadStatus('단일 파일 업로드를 처리 중입니다...', 'running');
	        const data = await fetchJson('/debug/api/rag/ingest-file', { method: 'POST', body: form });
	        applyUploadIdentity(data.result);
	        renderUploadRows([{ fileName: file.name, source: data.result.source, version: data.result.version, title: data.result.title, chunkCount: data.result.chunkCount, traceId: data.result.traceId, stored: data.result.stored, message: 'stored' }]);
	        setUploadStatus(`단일 파일 업로드 완료\nsource=${data.result.source}\nversion=${data.result.version}\nchunks=${data.result.chunkCount}\ntrace=${data.result.traceId || '-'}`, 'success');
	        await loadTraceById(data.result.traceId);
	        alert(`업로드 완료: ${file.name}`);
	    } catch (e) {
	        renderUploadRows([{ fileName: file.name, source: '-', version: '-', title: '-', chunkCount: 0, stored: false, message: e.message }]);
	        setUploadStatus('단일 파일 업로드 실패\n' + e.message, 'error');
	    } finally {
	        setLoading(false);
	    }
	}

	async function uploadMultiFiles() {
	    const files = Array.from(document.getElementById('ragFiles').files || []);
	    if (files.length === 0) {
	        setUploadStatus('멀티파일 업로드 실패\n파일을 먼저 선택하세요.', 'error');
	        return;
	    }
	    const payload = currentRagPayload();
	    const form = new FormData();
	    form.append('category', payload.category);
	    form.append('source', payload.source);
	    form.append('version', payload.version);
	    form.append('title', payload.title);
	    files.forEach(file => form.append('files', file));
	    try {
	        setLoading(true, `멀티파일 업로드 중... (${files.length}개)`);
	        setUploadStatus(`멀티파일 업로드를 처리 중입니다...\n선택 파일 수=${files.length}`, 'running');
	        const data = await fetchJson('/debug/api/rag/ingest-files', { method: 'POST', body: form });
	        applyUploadIdentity(data.result);
	        renderUploadRows(data.result.files || []);
	        setUploadStatus(`멀티파일 업로드 완료\nsource=${data.result.source}\nversion=${data.result.version}\n성공=${data.result.successCount}, 실패=${data.result.failCount}, chunks=${data.result.totalChunkCount}\ntrace=${data.result.traceId || '-'}`, data.result.failCount > 0 ? 'running' : 'success');
	        await loadTraceById(data.result.traceId);
	        alert(`멀티파일 업로드 완료: 성공 ${data.result.successCount}건 / 실패 ${data.result.failCount}건`);
	    } catch (e) {
	        setUploadStatus('멀티파일 업로드 실패\n' + e.message, 'error');
	    } finally {
	        setLoading(false);
	    }
	}


	function formatTraceDetails(details) {
	    if (!details || Object.keys(details).length === 0) return '-';
	    return Object.entries(details).map(([k,v]) => `${k}=${typeof v === 'object' ? JSON.stringify(v) : v}`).join(', ');
	}

	function renderRagTraceResponse(data) {
	    const entries = data.entries || [];
	    const summaries = data.summaries || (data.summary ? [data.summary] : []);
	    document.getElementById('ragTraceSummary').textContent = `trace ${summaries.length}건 / entry ${entries.length}건`;
	    if (entries.length === 0) {
	        document.getElementById('ragTracePanel').textContent = '조회된 RAG trace가 없습니다.';
	        return;
	    }
	    const lines = [];
	    summaries.forEach(summary => {
	        if (!summary) return;
	        lines.push(`[TRACE ${summary.traceId}] ${summary.operation} | ${summary.finalStatus} | ${summary.lastStage} | ${summary.lastMessage}`);
	    });
	    if (lines.length > 0) lines.push('');
	    entries.forEach(entry => {
	        lines.push(`[${entry.timestamp}] [${entry.traceId}] ${entry.operation} > ${entry.stage} > ${entry.status} :: ${entry.message}`);
	        lines.push(`  - ${formatTraceDetails(entry.details)}`);
	    });
	    document.getElementById('ragTracePanel').textContent = lines.join('
');
	}

	async function loadLatestRagTraces() {
	    try {
	        const limit = document.getElementById('ragTraceLimit').value || '150';
	        renderRagTraceResponse(await fetchJson(`/debug/api/rag/traces?limit=${encodeURIComponent(limit)}`));
	    } catch (e) {
	        document.getElementById('ragTracePanel').textContent = 'RAG trace 조회 실패: ' + e.message;
	    }
	}

	async function loadTraceById(traceIdValue) {
	    try {
	        const traceId = traceIdValue || (document.getElementById('ragTraceId').value || '').trim();
	        if (!traceId) {
	            document.getElementById('ragTracePanel').textContent = '조회할 Trace ID를 입력하세요.';
	            return;
	        }
	        document.getElementById('ragTraceId').value = traceId;
	        renderRagTraceResponse(await fetchJson(`/debug/api/rag/traces/${encodeURIComponent(traceId)}`));
	    } catch (e) {
	        document.getElementById('ragTracePanel').textContent = 'Trace 상세 조회 실패: ' + e.message;
	    }
	}

	async function clearRagTraces() {
	    try {
	        await fetchJson('/debug/api/rag/traces/clear', { method: 'POST' });
	        document.getElementById('ragTraceSummary').textContent = 'RAG trace 비움 완료';
	        document.getElementById('ragTracePanel').textContent = '아직 조회하지 않음';
	    } catch (e) {
	        document.getElementById('ragTracePanel').textContent = 'RAG trace 비우기 실패: ' + e.message;
	    }
	}

	function selectRagTrace(traceId) {
	    document.getElementById('ragTraceId').value = traceId;
	    loadTraceById(traceId);
	}

	window.addEventListener('load', async () => {
	    await loadDebugConfig();
	    await loadOllamaModelConfig();
	    await loadDbInfo();
	    await loadLatestRagTraces();
	});
	
	function setOllamaModelStatus(text) {
	    document.getElementById("ollamaModelStatus").textContent = text;
	}

	function fillModelSelect(selectId, models, selectedValue) {
	    const select = document.getElementById(selectId);
	    select.innerHTML = "";

	    const emptyOption = document.createElement("option");
	    emptyOption.value = "";
	    emptyOption.textContent = "(선택 안 함)";
	    select.appendChild(emptyOption);

	    models.forEach(model => {
	        const option = document.createElement("option");
	        option.value = model.name;
	        option.textContent = model.displayName || model.name;
	        if (selectedValue && selectedValue === model.name) {
	            option.selected = true;
	        }
	        select.appendChild(option);
	    });
	}

	function renderOllamaModelTable(models) {
	    const table = document.getElementById('ollamaModelTable');
	    if (!models || models.length === 0) {
	        table.innerHTML = '<tr><td colspan="4">조회된 모델이 없습니다.</td></tr>';
	        document.getElementById('ollamaModelSummary').textContent = '모델 0건';
	        return;
	    }
	    table.innerHTML = models.map(model => `
	        <tr>
	            <td>${model.displayName || model.name}</td>
	            <td>${model.name || '-'}</td>
	            <td>${model.state || '-'}</td>
	            <td>${model.size ? model.size.toLocaleString() : '-'}</td>
	        </tr>`).join('');
	    const running = models.filter(m => (m.state || '').includes('RUNNING')).length;
	    const installed = models.filter(m => (m.state || '').includes('INSTALLED')).length;
	    document.getElementById('ollamaModelSummary').textContent = `전체 ${models.length}건 / running 포함 ${running} / installed 포함 ${installed}`;
	}

	function applyOllamaFilters() {
	    const keyword = (document.getElementById('ollamaSearchKeyword').value || '').trim().toLowerCase();
	    const state = document.getElementById('ollamaStateFilter').value;
	    const filtered = allOllamaModels.filter(model => {
	        const target = `${model.name || ''} ${model.displayName || ''}`.toLowerCase();
	        const keywordMatched = !keyword || target.includes(keyword);
	        const stateMatched = state === 'ALL' || (model.state || '').includes(state);
	        return keywordMatched && stateMatched;
	    });
	    fillModelSelect("travelSearchModel", filtered, document.getElementById("travelSearchModel").value);
	    fillModelSelect("travelPlanModel", filtered, document.getElementById("travelPlanModel").value);
	    fillModelSelect("generalModel", filtered, document.getElementById("generalModel").value);
	    fillModelSelect("devModel", filtered, document.getElementById("devModel").value);
	    fillModelSelect("miceModel", filtered, document.getElementById("miceModel").value);
	    renderOllamaModelTable(filtered);
	}

	async function loadOllamaModels() {
	    const source = document.getElementById("ollamaModelSource").value;

	    try {
	        const data = await fetchJson(`/debug/api/ollama/models?source=${encodeURIComponent(source)}`);
	        allOllamaModels = data || [];
	        applyOllamaFilters();
	        setOllamaModelStatus(`모델 목록 로드 완료 (${source}) / 개수=${allOllamaModels.length}`);
	        const travelSearch = document.getElementById("travelSearchModel").value;
	        if (travelSearch && travelSearch.includes("azure99/blossom-v6.3")) {
	            setOllamaModelStatus(`모델 목록 로드 완료 (${source}) / 개수=${allOllamaModels.length}
경고: blossom-v6.3는 현재 travel search tool 호출에 사용할 수 없습니다.`);
	        }
	    } catch (e) {
	        setOllamaModelStatus("모델 목록 조회 실패: " + e.message);
	    }
	}

	async function loadOllamaModelConfig() {
	    try {
	        const config = await fetchJson('/debug/api/ollama/config');

	        document.getElementById("ollamaModelSource").value = config.modelSource || "RUNNING";

	        await loadOllamaModels();

	        document.getElementById("travelSearchModel").value = config.travelSearchModel || "";
	        document.getElementById("travelPlanModel").value = config.travelPlanModel || "";
	        document.getElementById("generalModel").value = config.generalModel || "";
	        document.getElementById("devModel").value = config.devModel || "";
	        document.getElementById("miceModel").value = config.miceModel || "";

	        setOllamaModelStatus(
	            `모델 설정 조회 완료
				source=${config.modelSource}
				travelSearch=${config.travelSearchModel || "(none)"}
				travelPlan=${config.travelPlanModel || "(none)"}
				general=${config.generalModel || "(none)"}
				dev=${config.devModel || "(none)"}
				mice=${config.miceModel || "(none)"}`
	        );
	    } catch (e) {
	        setOllamaModelStatus("모델 설정 조회 실패: " + e.message);
	    }
	}

	async function saveOllamaModelConfig() {
	    const payload = {
	        modelSource: document.getElementById("ollamaModelSource").value,
	        travelSearchModel: document.getElementById("travelSearchModel").value,
	        travelPlanModel: document.getElementById("travelPlanModel").value,
	        generalModel: document.getElementById("generalModel").value,
	        devModel: document.getElementById("devModel").value,
	        miceModel: document.getElementById("miceModel").value
	    };

	    if (payload.travelSearchModel && payload.travelSearchModel.includes("azure99/blossom-v6.3")) {
	        setOllamaModelStatus("저장 중단: blossom-v6.3는 현재 travel search tool 호출용으로 사용할 수 없습니다.");
	        return;
	    }

	    try {
	        const config = await fetchJson('/debug/api/ollama/config', {
	            method: 'POST',
	            headers: {'Content-Type': 'application/json'},
	            body: JSON.stringify(payload)
	        });

	        setOllamaModelStatus(
	            `모델 설정 저장 완료
				source=${config.modelSource}
				travelSearch=${config.travelSearchModel || "(none)"}
				travelPlan=${config.travelPlanModel || "(none)"}
				general=${config.generalModel || "(none)"}
				dev=${config.devModel || "(none)"}
				mice=${config.miceModel || "(none)"}`
	        );
	    } catch (e) {
	        setOllamaModelStatus("모델 설정 저장 실패: " + e.message);
	    }
	}

	async function resetOllamaModelConfig() {
	    try {
	        const config = await fetchJson('/debug/api/ollama/config/reset', {
	            method: 'POST'
	        });

	        document.getElementById("ollamaModelSource").value = config.modelSource || "RUNNING";
	        await loadOllamaModels();

	        document.getElementById("travelSearchModel").value = config.travelSearchModel || "";
	        document.getElementById("travelPlanModel").value = config.travelPlanModel || "";
	        document.getElementById("generalModel").value = config.generalModel || "";
	        document.getElementById("devModel").value = config.devModel || "";
	        document.getElementById("miceModel").value = config.miceModel || "";

	        setOllamaModelStatus("모델 설정 초기화 완료");
	    } catch (e) {
	        setOllamaModelStatus("모델 설정 초기화 실패: " + e.message);
	    }
	}