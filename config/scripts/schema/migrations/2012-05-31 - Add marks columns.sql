
ALTER TABLE feedback
ADD actualMark  NUMBER(5, 0);

ALTER TABLE feedback
ADD actualGrade nvarchar2(255);

ALTER TABLE feedback
ADD agreedMark  NUMBER(5, 0);

ALTER TABLE feedback
ADD agreedGrade nvarchar2(255);