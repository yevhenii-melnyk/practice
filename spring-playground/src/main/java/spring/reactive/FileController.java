package spring.reactive;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;

@RestController
public class FileController {

	@GetMapping("/file")
	public Mono<Void> getFile(ServerHttpRequest request, ServerHttpResponse response) {
		HttpRange range = request.getHeaders().getRange().get(0);
		try {
			ZeroCopyHttpOutputMessage zeroCopyResponse = (ZeroCopyHttpOutputMessage) response;
			Resource resource = new ClassPathResource("fish.mp4");
			File file = resource.getFile();
			long length = file.length();
			long chunk = length / 4;
			HttpHeaders headers = zeroCopyResponse.getHeaders();
			headers.set("Accept-Ranges", "bytes");
			headers.setContentType(new MediaType("video", "mp4"));
			long rangeStart = range.getRangeStart(length);
			long rangeEnd = Math.min(rangeStart + chunk, length - 1);
			headers.setContentLength(chunk);
			if (rangeEnd < length)
				response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
			headers.set("Content-Range", String.format("bytes %d-%d/%d", rangeStart, rangeEnd, length));
			headers.setCacheControl("no-cache");
			headers.setContentDisposition(ContentDisposition.parse("inline"));
			return zeroCopyResponse.writeWith(file, rangeStart, chunk);
		} catch (Throwable ex) {
			return Mono.error(ex);
		}
	}

	@GetMapping("/resource")
	public ResponseEntity<Resource> resource() throws IOException {
		Resource resource = new ClassPathResource("fish.mp4");
		return ResponseEntity.ok()
				.contentType(new MediaType("video", "mp4"))
				.header("Content-Disposition", "attachment")
				.body(resource);
	}

	@GetMapping("/writeWith")
	public Mono<Void> writeWith(ServerHttpResponse response) throws IOException {
		Resource resource = new ClassPathResource("fish.mp4");
		ZeroCopyHttpOutputMessage zeroCopyResponse = (ZeroCopyHttpOutputMessage) response;
		response.getHeaders().set("Content-Disposition", "attachment");
		response.getHeaders().setContentType(new MediaType("video", "mp4"));
		File file = resource.getFile();
		return zeroCopyResponse.writeWith(file, 0, file.length());
	}

}
