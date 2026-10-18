# Quy tac lam viec cho AI Story Studio

## Pham vi

- Truoc khi sua, doc `docs/BACKLOG_V1.md` va `docs/TASK_LOG.md` neu file da co.
- Moi lan chi lam mot backlog ID. Khong tu chuyen sang ID tiep theo hoac them tinh nang ngoai pham vi duoc giao.
- Kiem tra cau hinh va ma nguon hien co truoc khi thay doi. Uu tien sua nguyen nhan goc va thay doi nho, co the review.
- Bao toan du lieu nguoi dung. Tuyet doi khong dung destructive Room migration hoac xoa du lieu cu de lam build/test chay duoc.

## Bao mat va ma nguon

- Khong hardcode, ghi log, commit hoac dua API key vao artifact loi.
- Khong ghi noi dung truyen rieng tu hoac thong tin nhay cam vao log/test report.
- Khong commit `local.properties`, API key, file `.jks`, `.keystore` hoac signing secrets.
- Moi comment moi trong code phai viet bang tieng Viet khong dau. Uu tien ten ham va ten bien ro nghia thay cho comment khong can thiet.

## Build va kiem thu

- Dung Gradle Wrapper cua project; kiem tra JDK, Android SDK va thiet bi/emulator truoc khi chay lenh.
- Chi ghi PASS khi lenh da chay xong va co ket qua that. Neu khong the chay do thiet bi, SDK hoac moi truong, ghi BLOCKED va neu ro bang chung.
- Ghi ma backlog, file tao/sua, ly do, lenh, ket qua test, artifact va rui ro con lai vao `docs/TASK_LOG.md`.
- Tao Git checkpoint cuc bo sau khi hoan tat phan viec duoc giao. Khong push, merge hoac phat hanh neu chua duoc yeu cau.

## Kien truc va backlog

- Theo stack va thu tu phu thuoc trong `docs/BACKLOG_V1.md`; khong them Room, navigation, provider AI hoac backup truoc task tuong ung.
- Khi thay doi schema Room: bat export schema, luu schema, viet migration va migration test; khong them fallback xoa schema.
- Khi task chua dat tieu chi nghiem thu, bao ro muc da dat va dieu kien con thieu; khong danh dau task hoan tat.
