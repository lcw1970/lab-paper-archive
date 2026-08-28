-- V3: 전문검색 인프라 + 다운로드 액션 구분

-- 1) PDF 본문 텍스트 저장 컬럼
ALTER TABLE papers ADD COLUMN content_text text;

-- 2) 전문검색용 tsvector (generated column)
--    'simple' 사용: 한국어 형태소 분석기가 없으므로 어간 추출 없이 토큰 그대로 색인한다.
ALTER TABLE papers ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')),         'A') ||
        setweight(to_tsvector('simple', coalesce(authors, '')),       'B') ||
        setweight(to_tsvector('simple', coalesce(venue, '')),         'C') ||
        setweight(to_tsvector('simple', coalesce(abstract_text, '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(content_text, '')),  'D')
        ) STORED;

CREATE INDEX idx_papers_search_vector ON papers USING GIN (search_vector);

-- 3) 부분 일치(LIKE '%키워드%') 보조 인덱스
--    tsvector는 단어 단위라 '트랜스'로 '트랜스포머'를 못 찾는다. pg_trgm으로 보완.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_papers_title_trgm ON papers USING GIN (title gin_trgm_ops);
CREATE INDEX idx_papers_authors_trgm ON papers USING GIN (authors gin_trgm_ops);

-- 4) 본문 추출 상태 인덱스 — 재색인 배치가 PENDING/FAILED만 훑도록
CREATE INDEX idx_papers_extract_status ON papers (extract_status)
    WHERE deleted_at IS NULL;

-- 5) 다운로드 로그에 열람/다운로드 구분 추가
ALTER TABLE download_logs ADD COLUMN action varchar(10) NOT NULL DEFAULT 'DOWNLOAD';
COMMENT ON COLUMN download_logs.action IS 'VIEW=브라우저 열람, DOWNLOAD=파일 저장';
