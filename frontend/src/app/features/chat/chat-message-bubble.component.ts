import { Component, Input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ChatMessage } from '../../core/models/chat.model';

@Component({
  selector: 'app-chat-message-bubble',
  standalone: true,
  imports: [DatePipe],
  template: `
    <div class="chat-row" [class.mine]="message.mine">
      <div class="chat-bubble">
        @if (!message.mine) {
          <strong>{{ message.senderFullName }}</strong>
        }
        <p>{{ message.content }}</p>
        <small>{{ message.createdAt | date:'HH:mm' }} · read {{ message.readByCount }}</small>
      </div>
    </div>
  `
})
export class ChatMessageBubbleComponent {
  @Input({ required: true }) message!: ChatMessage;
}
