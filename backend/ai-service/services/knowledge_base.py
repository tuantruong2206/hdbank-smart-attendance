"""Knowledge base for company policy Q&A using pgvector embeddings."""

import logging
from hashlib import sha256

from sqlalchemy import text

from models.database import get_db_session

logger = logging.getLogger(__name__)

# Simple TF-IDF-like embedding (384-dim mock for compatibility with pgvector)
# In production, replace with sentence-transformers Vietnamese model
EMBEDDING_DIM = 384


def _simple_embedding(text_content: str) -> list[float]:
    """Generate a deterministic pseudo-embedding from text content.

    This is a placeholder. In production, use:
        from sentence_transformers import SentenceTransformer
        model = SentenceTransformer('keepitreal/vietnamese-sbert')
        return model.encode(text_content).tolist()
    """
    import numpy as np

    seed = int(sha256(text_content.encode()).hexdigest()[:8], 16)
    rng = np.random.RandomState(seed)
    vec = rng.randn(EMBEDDING_DIM).astype(float)
    norm = np.linalg.norm(vec)
    return (vec / norm).tolist() if norm > 0 else vec.tolist()


# Default HDBank policy documents
DEFAULT_POLICIES = [
    {
        "title": "Quy định chấm công",
        "content": "Nhân viên phải chấm công vào/ra hàng ngày qua ứng dụng di động. "
        "Chấm công bằng WiFi BSSID (ưu tiên) hoặc GPS. Nhân viên nghiệp vụ chỉ được "
        "chấm công 1 lần/ca. Nhân viên IT có thể chấm công nhiều địa điểm, tối thiểu "
        "cách nhau 30 phút.",
        "category": "attendance",
    },
    {
        "title": "Quy định đi trễ có phép",
        "content": "Mỗi nhân viên được phép đi trễ tối đa 4 lần/tháng (configurable). "
        "Nếu đi trễ trong thời gian grace period (15 phút mặc định) và còn quota, sẽ "
        "được tính là 'trễ có phép'. Hết quota sẽ tính 'trễ không phép'. Quota tự động "
        "reset vào ngày 1 hàng tháng. HR có thể override cho trường hợp đặc biệt.",
        "category": "attendance",
    },
    {
        "title": "Quy định nghỉ phép",
        "content": "Phép năm: 12-20 ngày tùy thâm niên. Phép ốm: theo quy định lao động. "
        "Phép cá nhân: tối đa 3 ngày/năm. Đơn nghỉ phép phải được gửi trước ít nhất 3 "
        "ngày làm việc (trừ trường hợp khẩn cấp). Phê duyệt nhiều cấp: trưởng bộ phận → "
        "trưởng phòng. Nếu người duyệt vắng >3 ngày, đơn tự động chuyển lên cấp trên.",
        "category": "leave",
    },
    {
        "title": "Quy định làm thêm giờ (OT)",
        "content": "OT phải được phê duyệt trước. Hệ số OT: ngày thường x1.5, cuối tuần x2.0, "
        "ngày lễ x3.0. Ca đêm (21:00-05:00) cộng thêm 30% lương ca đêm. OT tối đa 40 "
        "giờ/tháng theo luật lao động.",
        "category": "overtime",
    },
    {
        "title": "Quy định bảng công",
        "content": "Bảng công hàng tháng được tính tự động từ dữ liệu chấm công. Trạng thái: "
        "Nháp → Chờ duyệt → Đã duyệt → Đã khóa. Sau khi khóa, dữ liệu được snapshot "
        "và không thể thay đổi. Nhân viên có thể xem bảng công cá nhân. Quản lý xem "
        "bảng công team. Admin khóa bảng công đã duyệt.",
        "category": "timesheet",
    },
    {
        "title": "Quy định leo thang (Escalation)",
        "content": "Nếu nhân viên không chấm công sau giờ bắt đầu ca + grace period, hệ thống "
        "tự động thông báo quản lý trực tiếp (cấp 1). Nếu sau 30 phút không có phản hồi, "
        "leo thang lên trưởng phòng (cấp 2). Cấp 3 lên giám đốc. Mỗi cấp leo thang có "
        "timeout riêng. Người nhận phải xác nhận VÀ thực hiện hành động.",
        "category": "escalation",
    },
    {
        "title": "Ngày nghỉ lễ 2026",
        "content": "Tết Dương lịch: 01/01. Tết Nguyên Đán: 25/01-31/01. Giỗ Tổ Hùng Vương: "
        "12/04. Giải phóng miền Nam: 30/04. Quốc tế Lao động: 01/05. Quốc khánh: 02/09. "
        "Các ngày lễ được cấu hình bởi admin.",
        "category": "holiday",
    },
    {
        "title": "Quy định bảo mật",
        "content": "Nhân viên không được chia sẻ tài khoản. Xác thực 2 yếu tố (2FA) khuyến khích "
        "sử dụng. Phát hiện gian lận chấm công (fake GPS, VPN, mock location) sẽ bị đánh "
        "dấu suspicious và báo cáo HR. Điểm rủi ro ≥70 tự động flag, ≥90 tự động leo thang.",
        "category": "security",
    },
]


def load_default_policies() -> int:
    """Load default policy documents into pgvector knowledge base.

    Returns the number of policies loaded.
    """
    session = get_db_session()
    try:
        # Check if vector extension exists
        try:
            session.execute(text("SELECT 1 FROM policy_embeddings LIMIT 1"))
        except Exception:
            session.rollback()
            logger.warning("policy_embeddings table not accessible, skipping knowledge base load")
            return 0

        # Check if already loaded
        count = session.execute(text("SELECT count(*) FROM policy_embeddings")).scalar()
        if count and count >= len(DEFAULT_POLICIES):
            logger.info("Knowledge base already loaded (%d documents)", count)
            return count

        loaded = 0
        for policy in DEFAULT_POLICIES:
            embedding = _simple_embedding(policy["content"])
            embedding_str = "[" + ",".join(str(x) for x in embedding) + "]"

            session.execute(
                text(
                    "INSERT INTO policy_embeddings (title, content, embedding, category, source_file) "
                    "VALUES (:title, :content, :embedding::vector, :category, :source) "
                    "ON CONFLICT DO NOTHING"
                ),
                {
                    "title": policy["title"],
                    "content": policy["content"],
                    "embedding": embedding_str,
                    "category": policy["category"],
                    "source": "default_policies",
                },
            )
            loaded += 1

        session.commit()
        logger.info("Loaded %d policy documents into knowledge base", loaded)
        return loaded
    except Exception:
        session.rollback()
        logger.exception("Failed to load knowledge base")
        return 0
    finally:
        session.close()


def search_policies(query: str, top_k: int = 3) -> list[dict]:
    """Search knowledge base using vector similarity.

    Returns top_k most relevant policy documents.
    """
    session = get_db_session()
    try:
        query_embedding = _simple_embedding(query)
        embedding_str = "[" + ",".join(str(x) for x in query_embedding) + "]"

        results = session.execute(
            text(
                "SELECT title, content, category, "
                "1 - (embedding <=> :query_vec::vector) AS similarity "
                "FROM policy_embeddings "
                "WHERE embedding IS NOT NULL "
                "ORDER BY embedding <=> :query_vec::vector "
                "LIMIT :top_k"
            ),
            {"query_vec": embedding_str, "top_k": top_k},
        ).fetchall()

        return [
            {
                "title": r[0],
                "content": r[1],
                "category": r[2],
                "similarity": round(float(r[3]), 4) if r[3] else 0,
            }
            for r in results
        ]
    except Exception:
        logger.exception("Knowledge base search failed")
        return []
    finally:
        session.close()
