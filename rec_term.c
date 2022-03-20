int fun(int n){
  if(n==0)
    return 0;
  putchar(64+n); 
  return fun(n-1);
}

int main(){
  fun(26); 
  putchar(10); 
  return 0;
}