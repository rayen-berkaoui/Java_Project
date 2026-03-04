USE tabaani_db;

UPDATE comments c
SET 
  likes_count = (SELECT COUNT(*) FROM activities a WHERE a.comment_id = c.id AND a.activity_type = 'LIKE'),
  dislikes_count = (SELECT COUNT(*) FROM activities a WHERE a.comment_id = c.id AND a.activity_type = 'DISLIKE')
WHERE c.id > 0;
