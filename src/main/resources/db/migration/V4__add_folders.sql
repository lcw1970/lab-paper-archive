CREATE TABLE folders (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_folders_name_lower ON folders (LOWER(name));

ALTER TABLE papers ADD COLUMN folder_id BIGINT;
ALTER TABLE papers
    ADD CONSTRAINT fk_papers_folder
    FOREIGN KEY (folder_id) REFERENCES folders (id) ON DELETE SET NULL;

CREATE INDEX ix_papers_folder ON papers (folder_id) WHERE deleted_at IS NULL;
