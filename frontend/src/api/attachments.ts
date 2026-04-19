import client from './client';
import type { Attachment } from '../types/api';

export const attachmentsApi = {
  upload: (file: File, comment?: string) => {
    const form = new FormData();
    form.append('file', file);
    if (comment) form.append('comment', comment);
    return client.post<Attachment>('/attachments', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getDownloadUrl: (attachmentId: number) => `/api/attachments/${attachmentId}`,
};
