package com.wfp.workflow.service;

import com.wfp.common.exception.BadRequestException;
import com.wfp.common.exception.ForbiddenException;
import com.wfp.common.exception.NotFoundException;
import com.wfp.workflow.dto.AttachmentDto;
import com.wfp.workflow.entity.Item;
import com.wfp.workflow.entity.ItemAttachment;
import com.wfp.workflow.repository.ItemAttachmentRepository;
import com.wfp.workflow.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final ItemRepository itemRepository;
    private final ItemAttachmentRepository attachmentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AttachmentDto> list(String key) {
        Item item = requireItem(key);
        return attachmentRepository.findMetadataByItem(item.getId(), item.getTenantId());
    }

    @Transactional
    public AttachmentDto add(String key, MultipartFile file, String actor) {
        Item item = requireItem(key);
        if (file.isEmpty()) {
            throw new BadRequestException("The file is empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Files can be at most 10 MB");
        }
        ItemAttachment attachment = attachmentRepository.save(ItemAttachment.builder()
                .tenantId(item.getTenantId())
                .itemId(item.getId())
                .fileName(safeFileName(file.getOriginalFilename()))
                .sizeBytes(file.getSize())
                .content(bytesOf(file))
                .uploadedBy(actor)
                .build());
        auditService.record("item.attachment.added", "ITEM", item.getKey(), actor,
                Map.of("fileName", attachment.getFileName(), "sizeBytes", attachment.getSizeBytes()));
        return new AttachmentDto(attachment.getId(), attachment.getFileName(), attachment.getSizeBytes(),
                attachment.getUploadedBy(), attachment.getUploadedAt());
    }

    @Transactional(readOnly = true)
    public ItemAttachment download(String key, UUID attachmentId) {
        return requireAttachment(requireItem(key), attachmentId);
    }

    @Transactional
    public void delete(String key, UUID attachmentId, String actor, boolean admin) {
        Item item = requireItem(key);
        ItemAttachment attachment = requireAttachment(item, attachmentId);
        if (!admin && !attachment.getUploadedBy().equals(actor)) {
            throw new ForbiddenException("Only the uploader or an admin can delete an attachment");
        }
        attachmentRepository.delete(attachment);
        auditService.record("item.attachment.deleted", "ITEM", item.getKey(), actor,
                Map.of("fileName", attachment.getFileName()));
    }

    private Item requireItem(String key) {
        return itemRepository.findByKey(key).orElseThrow(() -> new NotFoundException("Item", key));
    }

    private ItemAttachment requireAttachment(Item item, UUID attachmentId) {
        return attachmentRepository.findByIdAndItemIdAndTenantId(attachmentId, item.getId(), item.getTenantId())
                .orElseThrow(() -> new NotFoundException("Attachment", attachmentId));
    }

    private static String safeFileName(String original) {
        String path = original == null ? "" : original;
        String name = path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1)
                .replaceAll("\\p{Cntrl}", "")
                .strip();
        if (name.isEmpty()) {
            throw new BadRequestException("The file needs a name");
        }
        return name.length() <= MAX_FILE_NAME_LENGTH ? name : name.substring(name.length() - MAX_FILE_NAME_LENGTH);
    }

    private static byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
