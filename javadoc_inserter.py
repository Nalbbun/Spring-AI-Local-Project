import re
from pathlib import Path

ROOT = Path('/mnt/data/work_javadoc')
JAVA_FILES = [p for p in ROOT.rglob('*.java') if '/build/' not in str(p).replace('\\', '/')]
TYPE_RE = re.compile(r'\b(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)')
ANNOTATION_LINE_RE = re.compile(r'^\s*@')
CONTROL_PREFIXES = ('if', 'for', 'while', 'switch', 'catch', 'do', 'try', 'return', 'throw', 'assert', 'new')


def split_camel(name: str):
    return re.sub(r'(?<!^)(?=[A-Z])', ' ', name).replace('_', ' ').strip().lower()


def scan_lines(lines):
    stripped = []
    depth_start = []
    depth = 0
    in_block = False
    in_string = False
    in_char = False
    in_text = False
    for line in lines:
        depth_start.append(depth)
        out = []
        i = 0
        while i < len(line):
            ch = line[i]
            nxt = line[i + 1] if i + 1 < len(line) else ''
            tri = line[i:i + 3]
            if in_block:
                if ch == '*' and nxt == '/':
                    in_block = False
                    out.extend('  ')
                    i += 2
                else:
                    out.append(' ')
                    i += 1
            elif in_text:
                if tri == '"""':
                    in_text = False
                    out.extend('   ')
                    i += 3
                else:
                    out.append(' ')
                    i += 1
            elif in_string:
                if ch == '\\':
                    out.extend('  ')
                    i += 2
                elif ch == '"':
                    in_string = False
                    out.append(' ')
                    i += 1
                else:
                    out.append(' ')
                    i += 1
            elif in_char:
                if ch == '\\':
                    out.extend('  ')
                    i += 2
                elif ch == "'":
                    in_char = False
                    out.append(' ')
                    i += 1
                else:
                    out.append(' ')
                    i += 1
            else:
                if tri == '"""':
                    in_text = True
                    out.extend('   ')
                    i += 3
                elif ch == '/' and nxt == '/':
                    out.extend(' ' * (len(line) - i))
                    i = len(line)
                elif ch == '/' and nxt == '*':
                    in_block = True
                    out.extend('  ')
                    i += 2
                elif ch == '"':
                    in_string = True
                    out.append(' ')
                    i += 1
                elif ch == "'":
                    in_char = True
                    out.append(' ')
                    i += 1
                else:
                    out.append(ch)
                    i += 1
        s = ''.join(out)
        stripped.append(s)
        for ch in s:
            if ch == '{':
                depth += 1
            elif ch == '}':
                depth = max(0, depth - 1)
    return stripped, depth_start


class TypeDecl:
    def __init__(self, start, body_line, end, depth, kind, name):
        self.start = start
        self.body_line = body_line
        self.end = end
        self.depth = depth
        self.kind = kind
        self.name = name
        self.parent = None
        self.children = []

    @property
    def body_depth(self):
        return self.depth + 1


def gather_declaration(lines, stripped, i):
    paren_depth = 0
    parts = []
    body_line = None
    end_line = None
    j = i
    seen_non_annotation = False
    while j < len(lines):
        s = stripped[j]
        st = s.strip()
        parts.append(st)
        if st and not st.startswith('@'):
            seen_non_annotation = True
        for ch in s:
            if ch == '(':
                paren_depth += 1
            elif ch == ')':
                paren_depth = max(0, paren_depth - 1)
        if seen_non_annotation and paren_depth == 0 and '{' in s:
            body_line = j
            end_line = j
            break
        if seen_non_annotation and paren_depth == 0 and ';' in s:
            end_line = j
            break
        j += 1
    if end_line is None:
        end_line = i
    return end_line, body_line, ' '.join(parts)


def find_matching_end(depth_start, body_line, type_depth):
    for k in range(body_line + 1, len(depth_start)):
        if depth_start[k] == type_depth:
            return k - 1
    return len(depth_start) - 1


def find_types(lines, stripped, depth_start):
    i = 0
    decls = []
    while i < len(lines):
        s = stripped[i].strip()
        if s and TYPE_RE.search(s):
            end_line, body_line, decl = gather_declaration(lines, stripped, i)
            m = TYPE_RE.search(decl)
            if body_line is not None and m:
                kind, name = m.group(1), m.group(2)
                end = find_matching_end(depth_start, body_line, depth_start[i])
                decls.append(TypeDecl(i, body_line, end, depth_start[i], kind, name))
                i = body_line + 1
                continue
        i += 1
    decls_sorted = sorted(decls, key=lambda d: (d.start, d.end))
    stack = []
    for d in decls_sorted:
        while stack and not (stack[-1].start < d.start <= stack[-1].end):
            stack.pop()
        if stack:
            d.parent = stack[-1]
            stack[-1].children.append(d)
        stack.append(d)
    return decls_sorted


def line_in_child_type(line_no, type_decl):
    return any(child.start <= line_no <= child.end for child in type_decl.children)


def find_annotation_start(lines, stripped, depth_start, line_no, min_line=0):
    start = line_no
    while start > min_line and ANNOTATION_LINE_RE.match(stripped[start - 1].strip()) and depth_start[start - 1] == depth_start[line_no]:
        start -= 1
    return start


def has_existing_javadoc(lines, start):
    i = start - 1
    while i >= 0 and not lines[i].strip():
        i -= 1
    if i < 0 or '*/' not in lines[i]:
        return False
    while i >= 0:
        if '/**' in lines[i]:
            return True
        if '/*' in lines[i] and '/**' not in lines[i]:
            return False
        i -= 1
    return False


def infer_role(path: Path, kind: str, name: str):
    p = str(path).replace('\\', '/').lower()
    if '/src/test/java/' in p:
        return '대상 기능의 동작을 검증하는 테스트 클래스'
    if '/controller/' in p or name.endswith('Controller'):
        return 'HTTP 요청과 응답을 처리하는 컨트롤러'
    if '/service/' in p or name.endswith('Service'):
        return '도메인 로직과 운영 지원 기능을 수행하는 서비스'
    if '/config/' in p or name.endswith('Config') or name.endswith('Properties'):
        return '애플리케이션 설정과 빈 구성을 담당하는 설정 타입'
    if '/model/' in p or '/debug/model/' in p:
        return '계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델'
    if '/parser/' in p or name.endswith('Parser'):
        return '입력 데이터를 해석하여 구조화된 결과로 변환하는 파서'
    if '/resolver/' in p or name.endswith('Resolver'):
        return '조건에 따라 적절한 대상이나 값을 해석하는 리졸버'
    if '/registry/' in p or name.endswith('Registry'):
        return '구현체 또는 메타데이터를 등록하고 조회하는 레지스트리'
    if '/orchestrator/' in p or name.endswith('Orchestrator'):
        return '여러 단계의 처리를 조율하는 오케스트레이터'
    if '/workflow/' in p or name.endswith('Workflow'):
        return '순차 처리 흐름을 조합하고 실행하는 워크플로'
    if '/agent/' in p or name.endswith('Agent'):
        return '세부 업무를 분리하여 수행하는 에이전트'
    if '/memory/' in p:
        return '대화 메모리 규칙 또는 저장 처리를 담당하는 컴포넌트'
    if '/rag/' in p and '/controller/' not in p:
        return 'RAG 관련 처리와 관리 기능을 담당하는 컴포넌트'
    if kind == 'interface':
        return '구현체가 따라야 할 동작 계약을 정의하는 인터페이스'
    if kind == 'enum':
        return '선택 가능한 상태나 유형을 정의하는 열거형'
    if kind == 'record':
        return '불변 입력 및 출력 데이터를 담는 레코드 모델'
    return '애플리케이션 기능을 구성하는 타입'


def param_desc(name, ptype):
    n = name.lower()
    if n in {'message', 'userquery', 'query', 'prompt'}:
        return '사용자 입력 또는 질의 내용'
    if n == 'category':
        return '대상 카테고리 정보'
    if n == 'state':
        return '현재 처리 상태 정보'
    if n == 'emitter':
        return 'SSE 이벤트 전송 객체'
    if n == 'request':
        return 'HTTP 요청 객체'
    if n == 'session':
        return 'HTTP 세션 객체'
    if n == 'conversationid':
        return '대화 식별자'
    if n == 'model':
        return '대상 모델 이름'
    if n == 'context':
        return '처리에 필요한 컨텍스트 정보'
    if n == 'result':
        return '처리 결과 객체'
    if n == 'command':
        return '실행 명령 정보'
    if n in {'config', 'properties'}:
        return '설정 정보'
    if n == 'url':
        return '대상 URL'
    if n in {'file', 'files'}:
        return '처리 대상 파일 정보'
    if n in {'text', 'content', 'body'}:
        return '본문 또는 텍스트 내용'
    if n == 'input':
        return '입력 데이터'
    if n == 'id':
        return '식별자 값'
    if n.endswith('id'):
        return f'{name} 식별자 값'
    if ptype:
        pt = ptype.lower().replace(' ', '')
        if 'httpservletrequest' in pt:
            return 'HTTP 요청 객체'
        if 'httpsession' in pt:
            return 'HTTP 세션 객체'
        if 'sseemitter' in pt:
            return 'SSE 응답 객체'
        if 'list<' in pt:
            return f'{name} 목록 정보'
        if 'map<' in pt:
            return f'{name} 매핑 정보'
    return f'{name} 값'


def return_desc(rtype):
    if not rtype or rtype == 'void':
        return None
    low = rtype.lower().replace(' ', '')
    if low == 'boolean':
        return '처리 가능 여부 또는 조건 충족 여부'
    if low == 'string':
        return '처리 결과 문자열'
    if low.startswith('list<'):
        return '조회 또는 생성된 목록'
    if low.startswith('map<'):
        return '키와 값으로 구성된 결과 매핑'
    if low.startswith('optional<'):
        return '존재할 경우 처리 결과'
    if low.startswith('completablefuture<'):
        return '비동기 처리 결과'
    if 'sseemitter' in low:
        return 'SSE 응답 스트림 객체'
    return f'{rtype} 타입의 처리 결과'


def method_summary(name, is_constructor=False, is_test=False):
    if is_test:
        return '대상 기능의 동작을 검증한다.'
    if is_constructor:
        return '필수 의존성을 주입하여 객체를 생성한다.'
    n = name.lower()
    if n.startswith('get'):
        return '지정된 정보를 조회한다.'
    if n.startswith('set'):
        return '대상 값을 설정한다.'
    if n.startswith(('is', 'has')):
        return '조건 충족 여부를 확인한다.'
    if n.startswith('supports'):
        return '지원 여부를 확인한다.'
    if n.startswith('resolve'):
        return '입력 정보를 해석하여 결과를 결정한다.'
    if n.startswith('parse'):
        return '입력 데이터를 파싱하여 구조화한다.'
    if n.startswith('build'):
        return '필요한 결과 객체를 구성한다.'
    if n.startswith('create'):
        return '새 항목 또는 결과를 생성한다.'
    if n.startswith('update'):
        return '대상 값을 갱신한다.'
    if n.startswith(('delete', 'remove', 'purge')):
        return '대상 데이터를 제거한다.'
    if n.startswith(('find', 'search', 'list', 'load', 'read')):
        return '대상 정보를 조회한다.'
    if n.startswith(('execute', 'run')):
        return '핵심 처리 로직을 실행한다.'
    if n.startswith('handle'):
        return '요청 또는 상태를 처리한다.'
    if n.startswith(('send', 'publish')):
        return '대상 정보를 외부로 전송한다.'
    if n.startswith('complete'):
        return '처리를 완료 상태로 반영한다.'
    if n.startswith('to'):
        return '현재 상태를 다른 표현 형태로 변환한다.'
    if n.startswith('blanktodefault'):
        return '값이 비어 있을 때 기본값으로 대체한다.'
    return f'{name} 기능을 수행한다.'


def split_top_level(text, delimiter=','):
    result = []
    cur = []
    depth_angle = depth_paren = depth_brack = depth_brace = 0
    for ch in text:
        if ch == '<':
            depth_angle += 1
        elif ch == '>':
            depth_angle = max(0, depth_angle - 1)
        elif ch == '(':
            depth_paren += 1
        elif ch == ')':
            depth_paren = max(0, depth_paren - 1)
        elif ch == '[':
            depth_brack += 1
        elif ch == ']':
            depth_brack = max(0, depth_brack - 1)
        elif ch == '{':
            depth_brace += 1
        elif ch == '}':
            depth_brace = max(0, depth_brace - 1)
        if ch == delimiter and depth_angle == depth_paren == depth_brack == depth_brace == 0:
            part = ''.join(cur).strip()
            if part:
                result.append(part)
            cur = []
        else:
            cur.append(ch)
    part = ''.join(cur).strip()
    if part:
        result.append(part)
    return result


def strip_leading_annotations(text):
    s = text.strip()
    while s.startswith('@'):
        i = 1
        while i < len(s) and (s[i].isalnum() or s[i] in '._$'):
            i += 1
        if i < len(s) and s[i] == '(':
            depth = 1
            i += 1
            while i < len(s) and depth > 0:
                if s[i] == '(':
                    depth += 1
                elif s[i] == ')':
                    depth -= 1
                i += 1
        s = s[i:].lstrip()
    return s


def parse_method_signature(decl, type_name=None):
    d = ' '.join(decl.replace('{', ' { ').replace(';', ' ; ').split())
    d = strip_leading_annotations(d)
    if '(' not in d or ')' not in d:
        return None
    m = re.search(r'\((.*)\)', d)
    if not m:
        return None
    before = d[:m.start()].strip()
    if not before:
        return None
    name = before.split()[-1]
    if name in CONTROL_PREFIXES:
        return None
    prefix = before[:before.rfind(name)].strip()
    modifiers = ('public', 'protected', 'private', 'static', 'final', 'abstract', 'default', 'synchronized', 'native', 'strictfp')
    changed = True
    while changed:
        changed = False
        for mod in modifiers:
            if prefix.startswith(mod + ' '):
                prefix = prefix[len(mod) + 1:].strip()
                changed = True
    if prefix.startswith('<'):
        depth = 0
        idx = 0
        while idx < len(prefix):
            if prefix[idx] == '<':
                depth += 1
            elif prefix[idx] == '>':
                depth -= 1
                if depth == 0:
                    idx += 1
                    break
            idx += 1
        prefix = prefix[idx:].strip()
    rtype = None if (type_name and name == type_name) else (prefix or None)
    params = []
    param_block = m.group(1).strip()
    if param_block:
        for part in split_top_level(param_block):
            part_clean = strip_leading_annotations(part)
            if not part_clean:
                continue
            var = part_clean.split()[-1].replace('[]', '')
            ptype = part_clean[:part_clean.rfind(var)].strip() if var in part_clean else None
            params.append((var, ptype))
    return {'name': name, 'return_type': rtype, 'params': params}


def parse_record_params(lines, start, body_line):
    text = ' '.join(lines[i].strip() for i in range(start, body_line + 1))
    text = strip_leading_annotations(text)
    m = re.search(r'\brecord\s+\w+\s*\((.*)\)\s*\{', text)
    if not m:
        return []
    params = []
    for part in split_top_level(m.group(1)):
        part_clean = strip_leading_annotations(part)
        if part_clean:
            params.append(part_clean.split()[-1])
    return params


def make_class_doc(path: Path, lines, decl: TypeDecl):
    indent = re.match(r'\s*', lines[decl.start]).group(0)
    role = infer_role(path, decl.kind, decl.name)
    doc = [f'{indent}/**',
           f'{indent} * {decl.name}는 {role}이다.',
           f'{indent} * <p>주요 기능: {split_camel(decl.name)} 관련 책임을 수행한다.</p>',
           f'{indent} * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>']
    if decl.kind == 'record':
        for p in parse_record_params(lines, decl.start, decl.body_line):
            doc.append(f'{indent} * @param {p} {param_desc(p, None)}')
    doc.append(f'{indent} */')
    return '\n'.join(doc) + '\n'


def make_method_doc(indent, type_name, decl, is_test=False):
    info = parse_method_signature(decl, type_name)
    if not info:
        return None
    is_constructor = info['return_type'] is None and info['name'] == type_name
    doc = [f'{indent}/**', f'{indent} * {method_summary(info["name"], is_constructor=is_constructor, is_test=is_test)}']
    if info['params']:
        doc.append(f'{indent} *')
        for pname, ptype in info['params']:
            doc.append(f'{indent} * @param {pname} {param_desc(pname, ptype)}')
    rdesc = return_desc(info['return_type'])
    if rdesc:
        doc.append(f'{indent} * @return {rdesc}')
    doc.append(f'{indent} */')
    return '\n'.join(doc) + '\n'


def parse_field_names(decl):
    d = decl.strip().rstrip(';').strip()
    if '=' in d:
        d = d.split('=', 1)[0].strip()
    d = strip_leading_annotations(d)
    for prefix in ('public ', 'protected ', 'private ', 'static ', 'final ', 'transient ', 'volatile '):
        d = d.replace(prefix, '')
    tokens = d.split()
    if len(tokens) < 2:
        return []
    names_part = ' '.join(tokens[1:])
    names = []
    for part in split_top_level(names_part):
        name = part.strip().split()[-1].replace('[]', '')
        if name:
            names.append(name)
    return names


def field_doc(indent, names):
    return f'{indent}/** {", ".join(names)} 값을 보관한다. */\n'


modified = []
for path in JAVA_FILES:
    lines = path.read_text(encoding='utf-8').splitlines(True)
    stripped, depth_start = scan_lines(lines)
    types = find_types(lines, stripped, depth_start)
    insertions = []

    for td in types:
        doc_start = find_annotation_start(lines, stripped, depth_start, td.start)
        if not has_existing_javadoc(lines, doc_start):
            insertions.append((doc_start, make_class_doc(path, lines, td)))

    for td in types:
        i = td.body_line + 1
        while i <= td.end:
            if line_in_child_type(i, td):
                child = next(c for c in td.children if c.start <= i <= c.end)
                i = child.end + 1
                continue
            if depth_start[i] != td.body_depth:
                i += 1
                continue
            st = stripped[i].strip()
            if not st or st in {'{', '}'} or st == 'static' or st.startswith('static {'):
                i += 1
                continue
            start = find_annotation_start(lines, stripped, depth_start, i, td.body_line + 1)
            decl_end, body_line, decl = gather_declaration(lines, stripped, start)
            decl_clean = ' '.join(decl.split())
            decl_no_ann = strip_leading_annotations(decl_clean)
            if TYPE_RE.search(decl_no_ann):
                i = decl_end + 1
                continue
            if has_existing_javadoc(lines, start):
                i = decl_end + 1
                continue
            indent = re.match(r'\s*', lines[start]).group(0)
            is_test = '/src/test/java/' in str(path).replace('\\', '/')

            eq_pos = decl_no_ann.find('=')
            par_pos = decl_no_ann.find('(')
            is_field = decl_no_ann.endswith(';') and (par_pos == -1 or (eq_pos != -1 and eq_pos < par_pos))
            is_method = par_pos != -1 and not is_field

            if is_method:
                doc = make_method_doc(indent, td.name, decl_no_ann, is_test=is_test)
                if doc:
                    insertions.append((start, doc))
            elif is_field:
                names = parse_field_names(decl_no_ann)
                if names:
                    insertions.append((start, field_doc(indent, names)))
            i = decl_end + 1

    if insertions:
        new_lines = lines[:]
        for idx, doc in sorted(insertions, key=lambda x: x[0], reverse=True):
            new_lines.insert(idx, doc)
        path.write_text(''.join(new_lines), encoding='utf-8')
        modified.append((str(path.relative_to(ROOT)), len(insertions)))

build_gradle = ROOT / 'build.gradle'
bg = build_gradle.read_text(encoding='utf-8')
if 'tasks.withType(Javadoc).configureEach' not in bg:
    bg += """

tasks.withType(Javadoc).configureEach {
    options.encoding = 'UTF-8'
    options.charSet = 'UTF-8'
    options.memberLevel = org.gradle.external.javadoc.JavadocMemberLevel.PRIVATE
    if (JavaVersion.current().isJava9Compatible()) {
        options.addBooleanOption('html5', true)
    }
}
"""
    build_gradle.write_text(bg, encoding='utf-8')

summary_path = ROOT / 'JAVADOC_APPLIED_SUMMARY.md'
summary_lines = [
    '# Javadoc 적용 요약',
    '',
    f'- 수정된 Java 파일 수: {len(modified)}',
    '- 적용 범위: 클래스/인터페이스/enum/record 설명, 메서드/생성자 @param @return, 필드 설명',
    '- build.gradle에 UTF-8 기반 Javadoc 생성 설정 추가',
    '',
    '## 수정 파일 일부',
]
for rel, count in modified[:50]:
    summary_lines.append(f'- {rel} ({count}개 주석 블록 추가)')
summary_lines += ['', '## 생성 명령 예시', '```bash', './gradlew clean javadoc', '```']
summary_path.write_text('\n'.join(summary_lines) + '\n', encoding='utf-8')

print(f'modified_files={len(modified)}')
