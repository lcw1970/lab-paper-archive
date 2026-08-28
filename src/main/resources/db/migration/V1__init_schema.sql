-- ============================================================
-- V1: 초기 스키마
-- 주의: 이 파일은 한 번 적용되면 절대 수정하지 말 것.
--       변경이 필요하면 V2, V3... 새 파일을 추가한다.
-- ============================================================

-- ── updated_at 자동 갱신 함수 ──
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- users : 랩 구성원
-- ============================================================
CREATE TABLE users (
                       id          BIGSERIAL    PRIMARY KEY,
                       email       VARCHAR(255) NOT NULL,
                       password    VARCHAR(100) NOT NULL,              -- BCrypt 해시(60자)
                       name        VARCHAR(50)  NOT NULL,
                       role        VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
                       status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                       last_login_at TIMESTAMP,
                       created_at  TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at  TIMESTAMP    NOT NULL DEFAULT now(),

                       CONSTRAINT ck_users_role   CHECK (role   IN ('ADMIN', 'MEMBER')),
                       CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED'))
);

-- 대소문자 무관 이메일 중복 방지 (Kim@x.ac.kr / kim@x.ac.kr 동일 취급)
CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));
CREATE INDEX ix_users_status ON users (status);

COMMENT ON COLUMN users.status IS 'PENDING=승인대기, ACTIVE=정상, SUSPENDED=정지(졸업생)';


-- ============================================================
-- papers : 논문 메타데이터
-- ============================================================
CREATE TABLE papers (
                        id             BIGSERIAL     PRIMARY KEY,
                        title          VARCHAR(500)  NOT NULL,
                        authors        VARCHAR(1000),
                        venue          VARCHAR(300),                    -- 학회/저널명
                        pub_year       SMALLINT,
                        doi            VARCHAR(200),
                        abstract_text  TEXT,
                        memo           TEXT,                            -- 랩 내부 코멘트
                        extracted_text TEXT,                            -- PDF 본문 (전문검색용)
                        extract_status VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
                        uploader_id    BIGINT,
                        created_at     TIMESTAMP     NOT NULL DEFAULT now(),
                        updated_at     TIMESTAMP     NOT NULL DEFAULT now(),
                        deleted_at     TIMESTAMP,                       -- soft delete

                        CONSTRAINT fk_papers_uploader
                            FOREIGN KEY (uploader_id) REFERENCES users (id) ON DELETE SET NULL,
                        CONSTRAINT ck_papers_pub_year
                            CHECK (pub_year IS NULL OR pub_year BETWEEN 1800 AND 2200),
                        CONSTRAINT ck_papers_extract_status
                            CHECK (extract_status IN ('PENDING', 'DONE', 'FAILED', 'SKIPPED'))
);

-- 목록 기본 조회: 삭제 안 된 것만 최신순
CREATE INDEX ix_papers_active_created
    ON papers (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_papers_pub_year   ON papers (pub_year)    WHERE deleted_at IS NULL;
CREATE INDEX ix_papers_uploader   ON papers (uploader_id);

COMMENT ON COLUMN papers.extract_status IS 'PDF 본문 추출 상태. 비동기 처리 결과';
COMMENT ON COLUMN papers.deleted_at     IS 'NULL이 아니면 휴지통 상태';


-- ============================================================
-- paper_files : 실제 PDF 파일 (논문 1건에 버전별 N개)
-- ============================================================
CREATE TABLE paper_files (
                             id            BIGSERIAL    PRIMARY KEY,
                             paper_id      BIGINT       NOT NULL,
                             stored_path   VARCHAR(500) NOT NULL,            -- 루트 기준 상대경로 '2026/02/uuid.pdf'
                             original_name VARCHAR(255) NOT NULL,
                             file_size     BIGINT       NOT NULL,
                             sha256        CHAR(64)     NOT NULL,
                             content_type  VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
                             version       INTEGER      NOT NULL DEFAULT 1,
                             uploaded_at   TIMESTAMP    NOT NULL DEFAULT now(),

                             CONSTRAINT fk_paper_files_paper
                                 FOREIGN KEY (paper_id) REFERENCES papers (id) ON DELETE CASCADE,
                             CONSTRAINT ux_paper_files_stored_path UNIQUE (stored_path),
                             CONSTRAINT ux_paper_files_sha256      UNIQUE (sha256),   -- 중복 업로드 차단
                             CONSTRAINT ux_paper_files_version     UNIQUE (paper_id, version),
                             CONSTRAINT ck_paper_files_size        CHECK (file_size > 0)
);

CREATE INDEX ix_paper_files_paper ON paper_files (paper_id);

COMMENT ON COLUMN paper_files.sha256      IS '파일 해시. 전역 UNIQUE로 중복 업로드 감지';
COMMENT ON COLUMN paper_files.stored_path IS 'app.storage.root 기준 상대경로. 절대경로 저장 금지';


-- ============================================================
-- tags / paper_tags
-- ============================================================
CREATE TABLE tags (
                      id         BIGSERIAL   PRIMARY KEY,
                      name       VARCHAR(50) NOT NULL,
                      created_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_tags_name_lower ON tags (LOWER(name));

CREATE TABLE paper_tags (
                            paper_id BIGINT NOT NULL,
                            tag_id   BIGINT NOT NULL,

                            CONSTRAINT pk_paper_tags PRIMARY KEY (paper_id, tag_id),
                            CONSTRAINT fk_paper_tags_paper FOREIGN KEY (paper_id) REFERENCES papers (id) ON DELETE CASCADE,
                            CONSTRAINT fk_paper_tags_tag   FOREIGN KEY (tag_id)   REFERENCES tags   (id) ON DELETE CASCADE
);

CREATE INDEX ix_paper_tags_tag ON paper_tags (tag_id);


-- ============================================================
-- download_logs : 저작권 대응 감사 로그
-- ============================================================
CREATE TABLE download_logs (
                               id            BIGSERIAL    PRIMARY KEY,
                               user_id       BIGINT,
                               user_email    VARCHAR(255) NOT NULL,            -- 계정 삭제돼도 남도록 스냅샷
                               paper_file_id BIGINT,
                               paper_title   VARCHAR(500) NOT NULL,            -- 논문 삭제돼도 남도록 스냅샷
                               ip            VARCHAR(45),                      -- IPv6 대응
                               user_agent    VARCHAR(300),
                               downloaded_at TIMESTAMP    NOT NULL DEFAULT now(),

                               CONSTRAINT fk_download_logs_user
                                   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
                               CONSTRAINT fk_download_logs_file
                                   FOREIGN KEY (paper_file_id) REFERENCES paper_files (id) ON DELETE SET NULL
);

CREATE INDEX ix_download_logs_time ON download_logs (downloaded_at DESC);
CREATE INDEX ix_download_logs_user ON download_logs (user_id, downloaded_at DESC);


-- ============================================================
-- updated_at 트리거
-- ============================================================
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_papers_updated_at
    BEFORE UPDATE ON papers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
