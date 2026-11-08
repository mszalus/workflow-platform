ALTER TABLE wf_comments ADD COLUMN item_id UUID REFERENCES wf_item(id);
ALTER TABLE wf_comments ALTER COLUMN process_instance_id DROP NOT NULL;

UPDATE wf_comments comment
SET item_id = item.id
FROM wf_item item
WHERE item.run_id = comment.process_instance_id
  AND item.tenant_id = comment.tenant_id;

CREATE INDEX idx_comments_item ON wf_comments(item_id, created_at);
