-- 초기 구현에서 extracted_text와 content_text가 따로 생긴 상태를 정리한다.
-- 엔티티는 앞으로 content_text만 사용하고, 과거 값이 있다면 한 번 복사한다.
UPDATE papers
SET content_text = extracted_text
WHERE content_text IS NULL
  AND extracted_text IS NOT NULL;

DROP INDEX IF EXISTS idx_papers_search_vector;
ALTER TABLE papers DROP COLUMN search_vector;

ALTER TABLE papers ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')),         'A') ||
        setweight(to_tsvector('simple', coalesce(authors, '')),       'B') ||
        setweight(to_tsvector('simple', coalesce(venue, '')),         'C') ||
        setweight(to_tsvector('simple', coalesce(doi, '')),           'C') ||
        setweight(to_tsvector('simple', coalesce(abstract_text, '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(memo, '')),          'C') ||
        setweight(to_tsvector('simple', coalesce(content_text, '')),  'D')
    ) STORED;

CREATE INDEX idx_papers_search_vector ON papers USING GIN (search_vector);
