-- 既存の users テーブルにBAN/有効列を追加する移行スクリプト
ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS banned  BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS ban_reason TEXT,
  ADD COLUMN IF NOT EXISTS banned_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS banned_by_admin_id INT;

CREATE INDEX IF NOT EXISTS idx_users_banned     ON public.users(banned);
CREATE INDEX IF NOT EXISTS idx_users_banned_by  ON public.users(banned_by_admin_id);
