package project.plantly.domain.upload.storage;

import org.springframework.core.io.Resource;
import project.plantly.domain.upload.ImageFormat;

import java.util.Optional;

/**
 * 업로드된 파일의 저장소 게이트웨이.
 *
 * <p>인터페이스로 두는 이유는 <b>저장소가 바뀔 것을 이미 알고 있기 때문</b>이다. 지금은 로컬 디스크지만
 * 배포 인프라가 정해지면 S3 또는 (인증을 Supabase 로 옮기는 설계가 진행되면) Supabase Storage 가 된다.
 * 그때 바뀌는 것은 이 인터페이스의 구현과 {@code GET /api/v1/files/{key}} 의 응답 방식(파일을 직접
 * 흘려보내는 대신 서명 URL 로 302)뿐이고, <b>업로드 응답의 url 계약과 회사 DTO 는 그대로다.</b>
 *
 * <p>내용을 {@code byte[]} 로 받는다. 상한이 10MB 라 통째로 들고 있어도 문제가 없고, 어차피 형식
 * 판별이 앞부분을 읽어야 해서 스트림으로 받으면 호출부가 되감기를 신경 써야 한다.
 */
public interface FileStorage {

    /**
     * 파일을 저장하고 그 key 를 돌려준다. key 생성은 구현이 아니라 {@code FileKeys} 가 맡는다 —
     * 저장소가 바뀌어도 이미 발급된 URL 의 모양이 같아야 하기 때문이다.
     */
    String store(byte[] content, ImageFormat format);

    /** 없는 key 는 예외가 아니라 빈 값이다. "없음"은 이 층에서 정상 결과이고, 404 로 바꾸는 것은 서비스의 몫이다. */
    Optional<Resource> load(String key);

    /**
     * 지금은 호출부가 없다. 고아 파일 정리(GC)가 붙을 자리를 인터페이스에 미리 열어 둔 것이고,
     * 저장소를 갈아끼울 때 삭제 능력을 빠뜨리지 않게 하는 표식이기도 하다.
     */
    void delete(String key);
}
