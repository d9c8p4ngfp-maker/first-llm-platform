UPDATE knowledge_document SET sync_status = 'PENDING'
WHERE sync_status IN ('INDEXING', 'CRAWLING') AND deleted = 0;

UPDATE user_profile SET synthesis_status = 'IDLE'
WHERE synthesis_status = 'RUNNING';

INSERT INTO async_task (task_type, ref_id, ref_extra, status, created_at, updated_at)
SELECT 'DOC_INDEX', kd.id, kd.knowledge_base_id, 'PENDING', NOW(), NOW()
FROM knowledge_document kd
WHERE kd.sync_status = 'PENDING' AND kd.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM async_task at
      WHERE at.task_type = 'DOC_INDEX' AND at.ref_id = kd.id
        AND at.status IN ('PENDING', 'RUNNING')
  );
